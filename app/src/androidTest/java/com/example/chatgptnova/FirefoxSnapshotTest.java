package com.example.chatgptnova;

import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/** Separate real Firefox Android UI A/B on synthetic frozen HTML only. No production path. */
public final class FirefoxSnapshotTest extends FrozenPageSnapshotTest {
    private String shell(String command){return fixtureShell(command);}
    private static String quote(String s){return "'"+s.replace("'","'\\''")+"'";}
    private Set<String> downloads(){Set<String> paths=new HashSet<>();for(String s:shell("find /sdcard/Download -maxdepth 3 -type f -name '*.pdf'").split("\\r?\\n"))if(s.startsWith("/sdcard/Download/"))paths.add(s);return paths;}
    private boolean press(String text){AccessibilityNodeInfo n=find(instrument.getUiAutomation().getRootInActiveWindow(),text);for(int i=0;n!=null&&i<5;i++,n=n.getParent())if(n.isClickable()&&n.isEnabled())return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);return false;}
    @Test public void sameFrozenHtmlCanBeSavedAsPdfInFirefoxAndroid() throws Exception {
        org.junit.Assume.assumeTrue(activity.getPackageManager().getLaunchIntentForPackage("org.mozilla.firefox")!=null);
        FrozenPageSnapshot snapshot=freeze();mutate();main(()->exporter().cancel());
        byte[] html=snapshot.frozenHtml.getBytes(StandardCharsets.UTF_8);
        evidence("firefox-source.html",html);Set<String> before=downloads();
        try(ServerSocket server=new ServerSocket(0,8,java.net.InetAddress.getByName("127.0.0.1"))) {
            Thread serving=new Thread(()->{while(!server.isClosed())try(Socket socket=server.accept()){
                socket.setSoTimeout(3000);java.io.InputStream in=socket.getInputStream();int end=0;
                // Consume bounded HTTP framing only, never interpret or log headers.
                for(int i=0;i<16384&&end<4;i++){int c=in.read();if(c<0)break;end=c==((end==0||end==2)?13:10)?end+1:0;}
                java.io.OutputStream out=socket.getOutputStream();out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "+html.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.write(html);out.flush();
            }catch(Exception ignored){}} ,"firefox-fixture-server");serving.setDaemon(true);serving.start();
            instrument.getTargetContext().startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("http://127.0.0.1:"+server.getLocalPort()+"/frozen-page.html")).setPackage("org.mozilla.firefox").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            final long[] nextStartupAction={0};
            waitFor("Firefox frozen fixture",()->{
                AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();
                if(find(root,"Before snapshot marker")!=null)return true;
                long now=android.os.SystemClock.uptimeMillis();if(now<nextStartupAction[0])return false;
                // A closing system role window can remain in accessibility briefly.
                // Do not send repeated Back events through it into the browser.
                if(root!=null&&root.getPackageName()!=null&&root.getPackageName().toString().contains("permissioncontroller")) {
                    if(!press("Cancel")&&!press("Not now")&&!press("Don't allow"))instrument.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
                    nextStartupAction[0]=now+2000;return false;
                }
                // First-run UI may return to Home after loading the supplied tab.
                // Reopen only this fixture's visible Continue card, never other tabs.
                AccessibilityNodeInfo tab=find(root,snapshot.title);
                if(tab!=null&&"recent.tab.title".equals(tab.getViewIdResourceName())) {
                    press(snapshot.title);nextStartupAction[0]=now+2000;return false;
                }
                for(String label:new String[]{"Start browsing","Not now","Skip","Continue"})if(press(label)){nextStartupAction[0]=now+2000;break;}
                return false;
            });
            waitFor("Firefox menu",()->press("More options")||press("Menu"));
            final long[] nextMenuAction={0};
            waitFor("Firefox Save as PDF",()->{
                if(press("Save as PDF"))return true;
                // Firefox 157.0.1 publicly groups this action under MoreSettingsSubmenu.
                long now=android.os.SystemClock.uptimeMillis();if(now>=nextMenuAction[0]){press("More");nextMenuAction[0]=now+2000;}
                return false;
            });
            final byte[][] result={null};
            waitFor("Firefox actual PDF file",()->{
                press("Save"); // Handles an optional native document picker; no unrelated UI matching.
                for(String path:downloads())if(!before.contains(path))try{
                    byte[] bytes=fixtureShellBytes("cat "+quote(path));if(bytes.length>1000&&new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-")){
                        File pdf=evidence("firefox-frozen.pdf",bytes);
                        try(ParcelFileDescriptor check=ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY);android.graphics.pdf.PdfRenderer r=new android.graphics.pdf.PdfRenderer(check)){if(r.getPageCount()>1){result[0]=bytes;return true;}}
                    }
                }catch(Exception ignored){}return false;
            });assertNotNull(result[0]);
        }catch(Exception|AssertionError e){retainUi("firefox");throw e;}finally{
            // Resume the monitored Nova Activity before stopping the external browser;
            // ActivityScenario cannot close a transient external-window lifecycle state.
            instrument.getTargetContext().startActivity(new Intent(instrument.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            waitFor("Nova resumed after Firefox",()->{AccessibilityNodeInfo root=instrument.getUiAutomation().getRootInActiveWindow();return root!=null&&"com.example.chatgptnova".contentEquals(root.getPackageName());});
            instrument.waitForIdleSync();shell("am force-stop org.mozilla.firefox");
        }
    }
}
