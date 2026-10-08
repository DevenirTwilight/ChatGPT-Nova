#!/usr/bin/env python3
"""Private input/output audit. Prints only counts; never source text, titles or identifiers."""
import hashlib
import html.parser
import json
import sys
import zipfile


class Document(html.parser.HTMLParser):
    def __init__(self):
        super().__init__()
        self.articles = 0
        self.scripts = 0

    def handle_starttag(self, tag, attrs):
        self.articles += tag == 'article'
        self.scripts += tag == 'script'


def sha_stream(stream):
    digest = hashlib.sha256()
    while block := stream.read(32768):
        digest.update(block)
    return digest.hexdigest()


def audit(source, output):
    with zipfile.ZipFile(source) as src, zipfile.ZipFile(output) as out:
        conversations = []
        for name in src.namelist():
            leaf = name.rsplit('/', 1)[-1]
            if leaf.startswith('conversations') and leaf.endswith('.json'):
                conversations.extend(json.loads(src.read(name)))
        manifest = json.loads(out.read('manifest.json'))
        assert manifest['format'] == 'nova-separate-conversations-v1'
        assert manifest['succeeded'] == len(conversations)
        assert manifest['failed'] == 0
        assert len(set(out.namelist())) == len(out.namelist())
        assert out.testzip() is None
        assert len(out.namelist()) == len(manifest['sha256']) + 1
        for name, expected in manifest['sha256'].items():
            with out.open(name) as stream:
                assert sha_stream(stream) == expected
        leaf_names = {}
        for name in src.namelist():
            leaf_names.setdefault(name.rsplit('/', 1)[-1], []).append(name)
        reports = {}
        inventories = leaf_names.get('library_files.json', [])
        assert len(inventories) <= 1
        if inventories:
            for record in json.loads(src.read(inventories[0])):
                if record.get('library_artifact_type') != 'deep_research_report':
                    continue
                entries = leaf_names.get(record.get('file_id', '') + '.dat', [])
                if len(entries) != 1:
                    continue
                report = json.loads(src.read(entries[0]))
                state = report.get('widget_state', {})
                message = state.get('report_message', {})
                if (state.get('status') != 'completed'
                        or message.get('metadata', {}).get('is_complete') is not True):
                    continue
                reports.setdefault(record.get('origination_thread_id'), []).append(message)
        source_hashes = set()
        for name in src.namelist():
            if name.endswith('.dat'):
                with src.open(name) as stream:
                    source_hashes.add(sha_stream(stream))
        stats = dict(conversations=len(conversations), succeeded=manifest['succeeded'], failed=0,
                     plainRawMessagesChecked=0, plainRawMessageChars=0, researchReports=0,
                     exactResearchBodyChars=0, attachmentOriginalFiles=0,
                     missingAttachmentReferences=0, zipCrcSha256='passed',
                     androidOrPhysicalDeviceValidated=False)
        for conversation, result in zip(conversations, manifest['conversations']):
            title = conversation.get('title') or '无标题会话'
            assert result['title'] == title[:512]
            assert result['status'] == 'exported'
            directory = result['directory']
            assert '..' not in directory and '/' not in directory and '\\' not in directory
            md = out.read(directory + '/conversation.md').decode()
            page = out.read(directory + '/conversation.html').decode()
            assert page.startswith('<!doctype html>')
            document = Document()
            document.feed(page)
            assert document.scripts == 0
            assert document.articles == result['visibleMessages'] + result['researchReports']
            mapping = conversation['mapping']
            if result['scope'] == 'current-branch':
                head = conversation.get('current_node')
                if head not in mapping:
                    parents = {n.get('parent') for n in mapping.values()}
                    leaves = [key for key in mapping if key not in parents]
                    assert len(leaves) == 1
                    head = leaves[0]
                chain, seen = [], set()
                while head in mapping:
                    assert head not in seen
                    seen.add(head)
                    chain.append(mapping[head])
                    head = mapping[head].get('parent')
                chain.reverse()
            else:
                chain = list(mapping.values())
            visible = 0
            for node in chain:
                message = node.get('message')
                if not isinstance(message, dict):
                    continue
                content = message.get('content') or {}
                if content.get('content_type') == 'thoughts':
                    continue
                visible += 1
                parts = content.get('parts')
                if isinstance(parts, list) and all(isinstance(p, str) for p in parts):
                    text = ''
                    for part in parts:
                        text += ('\n' if text else '') + part
                    # References are intentionally represented by separate attachment descriptors.
                    if text and 'sediment://' not in text and 'file_' not in text:
                        assert text in md
                        stats['plainRawMessagesChecked'] += 1
                        stats['plainRawMessageChars'] += len(text)
            assert visible == result['visibleMessages']
            thread = conversation.get('conversation_id') or conversation.get('id')
            bound = reports.get(thread, [])
            assert len(bound) == result['researchReports']
            for report in bound:
                body = '\n'.join(report['content']['parts'])
                assert body in md
                stats['researchReports'] += 1
                stats['exactResearchBodyChars'] += len(body)
            attachments = [name for name in manifest['sha256']
                           if name.startswith(directory + '/attachments/')]
            assert len(attachments) == result['attachments']
            for name in attachments:
                assert manifest['sha256'][name] in source_hashes
            stats['attachmentOriginalFiles'] += len(attachments)
            stats['missingAttachmentReferences'] += result['missingAttachmentReferences']
        return stats


if __name__ == '__main__':
    if len(sys.argv) != 3:
        raise SystemExit('Usage: check-batch.py private-input.zip private-output.zip')
    try:
        print(json.dumps(audit(*sys.argv[1:]), indent=2))
    except Exception:
        raise SystemExit('Private batch audit failed; source details suppressed.')
