package com.example.chatgptnova;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/** Default build contract: no experimental instance, settings entry, or injection. */
public final class LegacyScannerDisabledTest extends FixtureActivity {
    @Before public void before() {assertFalse("Stable suite requires default flag",BuildConfig.ENABLE_LEGACY_SCANNER);start();}
    @After public void after() {if(scenario!=null)scenario.close();}
    @Test public void defaultPageAndRecreationNeverCreateOrInjectScanner() throws Exception {
        Field f=MainActivity.class.getDeclaredField("conversationExport");f.setAccessible(true);
        main(()->{try {assertNull(f.get(activity));}catch(Exception e){throw new AssertionError(e);}});
        for(String key:new String[]{"__novaHistoryScrollTrial","__novaExportCapture"}) assertEquals("undefined",js("typeof window."+key));
        scenario.recreate();scenario.onActivity(value->{activity=value;web=web(value);});fixture(PAGE);
        main(()->{try {assertNull(f.get(activity));}catch(Exception e){throw new AssertionError(e);}});
        assertEquals("undefined",js("typeof window.__novaHistoryScrollTrial"));
        main(()->assertThrows(IllegalStateException.class,()->new ConversationExport(activity,web,()->true)));
    }
    @Test public void defaultSettingsHaveNoExperimentalEntry() throws Exception {
        Method m=MainActivity.class.getDeclaredMethod("showSettings");m.setAccessible(true);
        main(()->{try {m.invoke(activity);}catch(Exception e){throw new AssertionError(e);}});
        Field f=MainActivity.class.getDeclaredField("settingsDialog");f.setAccessible(true);
        waitFor("settings ready",()->{try{return f.get(activity)!=null;}catch(Exception e){throw new AssertionError(e);}});
        main(()->{
            try {
                android.app.AlertDialog dialog=(android.app.AlertDialog)f.get(activity);
                android.widget.ListAdapter items=dialog.getListView().getAdapter();
                for(int i=0;i<items.getCount();i++) {
                    String label=items.getItem(i).toString();
                    assertFalse(label.contains("实验"));assertFalse(label.contains("Scanner"));assertFalse(label.contains("完整"));
                }
            } catch(Exception e){throw new AssertionError(e);}
        });
    }
}
