package com.example.chatgptnova;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.text.*;
import android.view.*;
import android.widget.*;
import com.example.chatgptnova.archive.*;
import java.io.*;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Native local library. Retained import owns only application Context, never an Activity. */
public final class ArchiveActivity extends Activity {
    static final int OPEN=7101;
    private LinearLayout root;private EditText search;private TextView status;private ListView list;
    private Button cancel,more,sort;private boolean earliest;private int limit=200;private String diagnostic="schema=1\nstage=idle";
    private Task task;private List<ArchiveStore.Row> rows=new ArrayList<>();private int listGeneration;private boolean destroyed;
    private final java.util.concurrent.ExecutorService listWorker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final AtomicInteger ACTIVE=new AtomicInteger();
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,16,16,16);setContentView(root);
        TextView heading=new TextView(this);heading.setText("Nova Archive");heading.setTextSize(24);root.addView(heading);
        TextView description=new TextView(this);description.setText("本地档案 · 数据来自你主动选择的 ChatGPT 官方导出文件。Nova 不读取在线历史；完整性取决于导入文件。");root.addView(description);
        Button open=button("导入 ChatGPT 数据");open.setOnClickListener(v->choose());
        cancel=button("取消导入");cancel.setVisibility(View.GONE);cancel.setOnClickListener(v->{if(task!=null)task.cancel();});
        status=new TextView(this);status.setText("尚未导入");root.addView(status);
        search=new EditText(this);search.setSingleLine(true);search.setHint("搜索会话标题");root.addView(search);
        sort=button("排序：最近更新时间");sort.setOnClickListener(v->{earliest=!earliest;sort.setText(earliest?"排序：最早时间":"排序：最近更新时间");limit=200;refresh();});
        list=new ListView(this);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));list.setOnItemClickListener((p,v,pos,id)->{Intent i=new Intent(this,ArchiveReaderActivity.class);i.putExtra("row",rows.get(pos).id);startActivity(i);});
        more=button("加载更多");more.setOnClickListener(v->{limit=Math.min(100000,limit+200);refresh();});
        LinearLayout actions=new LinearLayout(this);root.addView(actions);Button copy=new Button(this);copy.setText("复制诊断");actions.addView(copy);copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Nova Archive diagnostic",diagnostic));Toast.makeText(this,"已复制脱敏诊断",Toast.LENGTH_SHORT).show();});
        Button delete=new Button(this);delete.setText("删除本地档案");actions.addView(delete);delete.setOnClickListener(v->confirmDelete());
        if(state!=null){earliest=state.getBoolean("earliest");search.setText(state.getString("search",""));diagnostic=state.getString("diagnostic",diagnostic);sort.setText(earliest?"排序：最早时间":"排序：最近更新时间");}
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){limit=200;refresh();}public void afterTextChanged(Editable e){}});
        Object retained=getLastNonConfigurationInstance();if(retained instanceof Task){task=(Task)retained;task.attach(this);}
        else if(ACTIVE.get()==0)cleanTemporary(this);
        refresh();
    }
    private Button button(String text){Button b=new Button(this);b.setText(text);root.addView(b);return b;}
    private void choose(){if(ACTIVE.get()>0){Toast.makeText(this,"请等待或取消当前导入",Toast.LENGTH_SHORT).show();return;}Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/zip","application/x-zip-compressed","application/json","text/plain"});startActivityForResult(i,OPEN);}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=OPEN)return;if(result!=RESULT_OK||data==null||data.getData()==null){status.setText("未选择文件，档案未改变。");return;}startImport(data.getData());}
    void startImport(Uri uri) {if(ACTIVE.get()>0)return;task=new Task(getApplicationContext(),uri);task.attach(this);task.start();}
    private void update(Task t){if(destroyed||task!=t)return;cancel.setVisibility(t.done?View.GONE:View.VISIBLE);if(!t.done){status.setText("正在本地导入… 可取消");return;}
        if(t.error!=null){diagnostic="schema=1\nimportId="+t.id+"\nerror="+t.error.code;status.setText(t.error.code+"\n"+t.error.explanation());}
        else{diagnostic="importId="+t.id+"\n"+t.stats.diagnostic();status.setText("导入完成：新增 "+t.stats.newConversations+" 个会话，更新 "+t.stats.updatedConversations+" 个会话。\n完整性取决于导入数据，请人工核对。");}refresh();
    }
    void refresh(){if(destroyed)return;final int generation=++listGeneration;String query=search.getText().toString();boolean order=earliest;int cap=limit;
        listWorker.submit(()->{List<ArchiveStore.Row> result;try(ArchiveStore db=new ArchiveStore(getApplicationContext())){result=db.list(query,order,cap+1);}catch(RuntimeException e){runOnUiThread(()->{if(!destroyed&&generation==listGeneration)status.setText("A06_DATABASE_WRITE_FAILED：无法读取本地档案。");});return;}
            runOnUiThread(()->{if(destroyed||generation!=listGeneration)return;boolean hasMore=result.size()>cap;if(hasMore)result.remove(result.size()-1);rows=result;List<String> labels=new ArrayList<>();for(ArchiveStore.Row r:rows){Double date=r.updated!=null?r.updated:r.created;labels.add(r.title+"\n"+(date==null?"日期未知":DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date((long)(date*1000))))+" · "+r.count+" 条消息");}list.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels));more.setVisibility(hasMore?View.VISIBLE:View.GONE);});
        });
    }
    private void confirmDelete(){if(ACTIVE.get()>0){Toast.makeText(this,"请先取消导入并等待结束",Toast.LENGTH_SHORT).show();return;}new AlertDialog.Builder(this).setTitle("删除本地档案？").setMessage("删除 Nova Archive 数据库与导入临时文件。不会删除在线 ChatGPT 聊天，也不会删除你已保存到外部的导出文件。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{listWorker.submit(()->{boolean ok;synchronized(ArchiveStore.LOCK){ok=getApplicationContext().deleteDatabase(ArchiveStore.DATABASE);cleanTemporary(getApplicationContext());}runOnUiThread(()->{if(destroyed)return;status.setText(ok?"本地档案已删除":"A09_STORAGE_FAILED：无法删除本地档案。");diagnostic="schema=1\nstage=deleted";refresh();});});}).show();}
    @Override protected void onResume(){super.onResume();if(search!=null)refresh();}
    @Override public Object onRetainNonConfigurationInstance(){if(task!=null)task.detach(this);return task;}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("search",search.getText().toString());out.putBoolean("earliest",earliest);out.putString("diagnostic",diagnostic);}
    @Override protected void onDestroy(){destroyed=true;listGeneration++;listWorker.shutdownNow();if(task!=null){task.detach(this);if(!isChangingConfigurations())task.cancel();}super.onDestroy();}
    static void cleanTemporary(Context c){File[] files=new File(c.getCacheDir(),"nova-archive-import").listFiles();if(files!=null)for(File f:files)if(f.isFile())f.delete();}
    static final class Task {
        final Context context;final Uri uri;final String id=UUID.randomUUID().toString();final ArchiveImporter.Control control=new ArchiveImporter.Control();
        volatile boolean done;volatile ArchiveError error;volatile ArchiveStore.Stats stats;private ArchiveActivity listener;private Thread worker;
        Task(Context c,Uri u){context=c.getApplicationContext();uri=u;}
        synchronized void attach(ArchiveActivity a){listener=a;notifyUi();}
        synchronized void detach(ArchiveActivity a){if(listener==a)listener=null;}
        synchronized void notifyUi(){ArchiveActivity a=listener;if(a!=null)a.runOnUiThread(()->a.update(this));}
        void start(){ACTIVE.incrementAndGet();worker=new Thread(this::run,"NovaArchiveImport");worker.start();}
        void cancel(){control.cancelled.set(true);Thread t=worker;if(t!=null)t.interrupt();}
        private void run(){File temp=null;try {
            String name="selected-export";long size=-1;
            try(Cursor q=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){if(q!=null&&q.moveToFirst()){name=q.getString(0);if(!q.isNull(1))size=q.getLong(1);}}
            if(name==null)name="selected-export";name=name.replaceAll("[\\p{Cntrl}]"," ");if(name.length()>200)name=name.substring(0,200);
            String lower=name.toLowerCase(Locale.ROOT);boolean zip=lower.endsWith(".zip"),json=lower.endsWith(".json");if(!zip&&!json)throw new ArchiveError("A01_UNSUPPORTED_FILE");if(size>ArchiveImporter.FILE_LIMIT)throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");
            File dir=new File(context.getCacheDir(),"nova-archive-import");if(!dir.isDirectory()&&!dir.mkdirs())throw new ArchiveError("A09_STORAGE_FAILED");temp=File.createTempFile("import-",".tmp",dir);
            try(InputStream in=context.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(temp)){
                if(in==null)throw new ArchiveError("A09_STORAGE_FAILED");byte[] b=new byte[32768];long count=0;int n;while((n=in.read(b))!=-1){control.check();count+=n;if(count>ArchiveImporter.FILE_LIMIT)throw new ArchiveError("A05_ARCHIVE_TOO_LARGE");out.write(b,0,n);}
            }
            control.check();try(ArchiveStore db=new ArchiveStore(context)){stats=db.importFile(temp,zip,name,id,control);}
        }catch(ArchiveError e){error=e;}catch(Exception e){error=new ArchiveError(control.cancelled.get()?"A07_IMPORT_CANCELLED":"A09_STORAGE_FAILED");}
        finally{if(temp!=null)temp.delete();done=true;ACTIVE.decrementAndGet();notifyUi();}}
    }
}
