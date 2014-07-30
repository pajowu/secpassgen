package de.pajowu.secpassgen;

import android.app.Activity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.widget.EditText;
import android.content.DialogInterface;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;
import android.widget.TextView;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.SecretKeyFactory;
import java.security.spec.KeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.Cipher;
import android.util.Base64;
import java.lang.StackTraceElement;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.View;
import android.text.InputType;
import java.util.Random;
import android.app.ProgressDialog;
import java.security.InvalidKeyException;
import javax.crypto.IllegalBlockSizeException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.BadPaddingException;
import android.content.Context;
import android.text.method.PasswordTransformationMethod;
public class MainActivity extends Activity
{
    //By Inmobiliariasomera (Own work) [CC-BY-SA-3.0 (http://creativecommons.org/licenses/by-sa/3.0)], via Wikimedia Commons
    SeekBar hashroundbar = null;
    Integer hashroundbarvalue = 10000;
    TextView hashroundtext = null;
    Integer length = 64;
    SeekBar lengthbar;
    TextView lengthbartext;
    private SecretKey key;
    private String masterPW;
    private String serviceID;
    TinyDB tinydb;
    String returnstring;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        tinydb = new TinyDB(this);
        hashroundbar = (SeekBar) findViewById(R.id.hashroundbar);
        hashroundtext = (TextView) findViewById(R.id.hashroundtext);
        
        hashroundbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressChanged = 0;
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                progressChanged = progress + 10000;
                progress = progress/10000*10000;
                hashroundbar.setProgress(progress);
                hashroundbarvalue = progress + 10000;
            }
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            public void onStopTrackingTouch(SeekBar seekBar) {
                hashroundtext.setText(getString(R.string.hashroundbar_wv) + hashroundbarvalue); //getString(R.string.hashroundbar_wv);
            }
        });
        lengthbar = (SeekBar) findViewById(R.id.lengthbar);
        lengthbartext = (TextView) findViewById(R.id.lengthbartext);
        
        lengthbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressChanged = 0;
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                progressChanged = progress;
                hashroundbar.setProgress(progress);
                length = progress + 1;
            }
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            public void onStopTrackingTouch(SeekBar seekBar) {
                lengthbartext.setText(getString(R.string.pwlenght_wv) + length); //getString(R.string.pwlenght_wv);
            }
        });
        Button masterPWbtn = (Button) findViewById(R.id.masterpwbtn);
        masterPWbtn.setOnClickListener(masterPWListener);
        Button serviceIdBtn = (Button) findViewById(R.id.serviceidbtn);
        serviceIdBtn.setOnClickListener(serviceIdListener);
        Button genPWbtn = (Button) findViewById(R.id.genpwbtn);
        genPWbtn.setOnClickListener(genPWListener);
        masterPW = tinydb.getString("masterPW");
        if (masterPW != "") {
            masterPW = decryptMasterPW(masterPW);
        } else {
            masterPW = setMasterPW();

        }

    }

    private String decryptMasterPW(String encMasterPW){
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle(getString(R.string.dec_master_key)); //getString(R.string.dec_master_key);
        alert.setMessage(getString(R.string.enter_pin)); //getString(R.string.enter_pin);

        
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        alert.setView(input);

        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() { //getString(R.string.ok);
            public void onClick(DialogInterface dialog, int whichButton) {
                final String pin = input.getText().toString();
                final ProgressDialog progdialog = ProgressDialog.show(MainActivity.this, "", 
                    getString(R.string.loading), true); //getString(R.string.loading);
                Runnable runnable = new Runnable() 
                {
                    @Override
                    public void run() {
                        try {

                            decryptMasterPW_run(pin);
                            //showAlert("MasterPW",masterPW);
                        } catch(Exception nfe) {
                            MainActivity.this.runOnUiThread(new Runnable(){
                                public void run() {
                                    showAlert(getString(R.string.wrong_pin), getString(R.string.wrong_pin_message)); ////getString(R.string.wrong_pin); getString(R.string.wrong_pin_message);
                                }
                            });
                            //finish();
                        //progdialog.dismiss();
                        }
                        progdialog.dismiss();
                        MainActivity.this.runOnUiThread(new Runnable(){
                            public void run() {
                                Button serviceIdBtn = (Button) findViewById(R.id.serviceidbtn);
                                    //Button genPWbtn = (Button) findViewById(R.id.genpwbtn);
                                Button masterPWbtn = (Button) findViewById(R.id.masterpwbtn);
                                masterPWbtn.setText(getString(R.string.change_master_key)); ////getString(R.string.change_master_key);
                                serviceIdBtn.setEnabled(true);
                            }
                        });

                     //showAlert("Done","Decrypted Masterpassword. You can now use the app.");

                    }
                };
                new Thread(runnable).start();
                //MainActivity.this.runOnUiThread(runnable);
            }
        });

alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() { //getString(R.string.cancel);
  public void onClick(DialogInterface dialog, int whichButton) {

  }
});
alert.show();
return "";
}
private void decryptMasterPW_run(String pin) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeySpecException, BadPaddingException {
    Cipher cipher = Cipher.getInstance("AES");
    key = generateKey(pin.toCharArray(),Base64.decode(tinydb.getString("salt"), Base64.DEFAULT),100000,256);
    cipher.init(Cipher.DECRYPT_MODE, key);
    cipher.update(Base64.decode(tinydb.getString("masterPW"), Base64.DEFAULT));
    byte[] plaintext = cipher.doFinal();
    masterPW = new String(plaintext);

}
private String setMasterPW(){
    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

    alert.setTitle(getString(R.string.set_master_password)); //getString(R.string.set_master_password);
    alert.setMessage(getString(R.string.set_master_password_message));//getString(R.string.set_master_password_message);
    final EditText input = new EditText(MainActivity.this);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
    alert.setView(input);

    alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() { //getString(R.string.ok);
        public void onClick(DialogInterface dialog, int whichButton) {
            masterPW = input.getText().toString();
            if (masterPW.length() != 0) {
                AlertDialog.Builder pinalert = new AlertDialog.Builder(MainActivity.this);
                pinalert.setTitle(getString(R.string.set_pin)); 
                pinalert.setMessage(getString(R.string.set_pin_message)); 
                final EditText input = new EditText(MainActivity.this);

                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setTransformationMethod(PasswordTransformationMethod.getInstance());
                pinalert.setView(input);

                pinalert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        final String pin = input.getText().toString();
                        if (pin.length() != 0) {
                            final ProgressDialog progdialog = ProgressDialog.show(MainActivity.this, "", 
                                getString(R.string.loading), true);

                            Runnable runnable = new Runnable() 
                            {
                                @Override
                                public void run() {
                                    try{
                                        setMasterPWAndSalt(pin, masterPW);
                                    } catch (Exception e) {
                                        MainActivity.this.runOnUiThread(new Runnable(){
                                            public void run() {
                                                showAlert(getString(R.string.not_compatible), getString(R.string.not_compatible_message));

                                            }
                                        });
                                    }
                                    progdialog.dismiss();
                                    MainActivity.this.runOnUiThread(new Runnable(){
                                        public void run() {
                                            Button serviceIdBtn = (Button) findViewById(R.id.serviceidbtn);

                                            Button masterPWbtn = (Button) findViewById(R.id.masterpwbtn);
                                            masterPWbtn.setText(getString(R.string.change_master_key)); 
                                            serviceIdBtn.setEnabled(true);
                                        }
                                    });

                                }
                            };
                            new Thread(runnable).start();

                        }
                    }

                });

pinalert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() { //getString(R.string.cancel);
  public void onClick(DialogInterface dialog, int whichButton) {

  }
});
pinalert.show();
}
}
});
alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() { //getString(R.string.cancel);
  public void onClick(DialogInterface dialog, int whichButton) {

  }
});
alert.show();
return "";
}
private void setMasterPWAndSalt(String pin, String masterPW) throws NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, InvalidKeySpecException, NoSuchPaddingException, BadPaddingException{
    Random random = new Random();
    random.setSeed(System.currentTimeMillis());
    byte[] buf = new byte[20];
    random.nextBytes(buf);
    byte[] salt = buf;
    String saltb64 = Base64.encodeToString(buf, Base64.DEFAULT);
    tinydb.putString("salt",saltb64);
    key = generateKey(pin.toCharArray(),salt,100000,256);
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    cipher.update(masterPW.getBytes());
    byte[] plaintext = cipher.doFinal();
    String encb64 = Base64.encodeToString(plaintext, Base64.DEFAULT);
    tinydb.putString("masterPW", encb64);
}
private final OnClickListener masterPWListener = new OnClickListener() {
    public void onClick(View v) {
        setMasterPW();
    }
};
private final OnClickListener serviceIdListener = new OnClickListener() {
    public void onClick(View v) {
        //String string = getString("title","message");
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setTitle(getString(R.string.set_service_id)); //getString(R.string.set_service_id);
        alert.setMessage(getString(R.string.set_service_id_message)); //getString(R.string.set_service_id_message);
        final EditText input = new EditText(MainActivity.this);
        alert.setView(input);
        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() { //getString(R.string.ok);
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
            //showAlert("title",value);
                serviceID = value;
            //7String pw = genPW(masterPW, serviceID, hashroundbarvalue);
            //showAlert("title",pw);
                if (value.length() != 0) {
                    Button genPWbtn = (Button) findViewById(R.id.genpwbtn);
                    genPWbtn.setEnabled(true);
                }

            }
        });
        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() { //getString(R.string.cancel);
          public void onClick(DialogInterface dialog, int whichButton) {

          }
      });
        alert.show();

    }
};
private final OnClickListener genPWListener = new OnClickListener() {
    public void onClick(View v) {
        genPW(masterPW, serviceID, hashroundbarvalue);
        //showAlert("Password",servicePW);
    }
};
public void showAlert(String title, String message){
    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
    alert.setTitle(title);
    alert.setMessage(message);
    alert.show();
}
public void showAlertInt(String title, Integer message){
    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
    alert.setTitle(title);
    alert.setMessage(message.toString());
    alert.show();
}
public AlertDialog.Builder genAlert(String title, String message){
    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
    alert.setTitle(title);
    alert.setMessage(message);
    return alert;
}
public static SecretKey generateKey(char[] passphraseOrPin, byte[] salt, int iter, final int outputKeyLength) throws NoSuchAlgorithmException, InvalidKeySpecException {



    final int iterations = iter; 

    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    KeySpec keySpec = new PBEKeySpec(passphraseOrPin, salt, iterations, outputKeyLength);
    SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);
    return secretKey;
}
public String[] splitStringEvery(String s, int interval) {
    int arrayLength = (int) Math.ceil(((s.length() / (double)interval)));
    String[] result = new String[arrayLength];

    int j = 0;
    int lastIndex = result.length - 1;
    for (int i = 0; i < lastIndex; i++) {
        result[i] = s.substring(j, j + interval);
        j += interval;
    } //Add the last bit
    result[lastIndex] = s.substring(j);

    return result;
}
public static String byteArrayToHex(byte[] a) {
   StringBuilder sb = new StringBuilder(a.length * 2);
   for(byte b: a)
      sb.append(String.format("%02x", b & 0xff));
  return sb.toString();
}
public void genPW(final String mainPW, final String servID, final Integer iterations){
    //showAlert("servID",servID);
    //showAlert("serviceID",serviceID);
    final ProgressDialog progdialog = ProgressDialog.show(MainActivity.this, "", 
        getString(R.string.loading), true); //getString(R.string.loading);
    Runnable runnable = new Runnable() 
    {
        @Override
        public void run() {
            try{
                if (length <= 32) {
                    key = generateKey(mainPW.toCharArray(),servID.getBytes(),iterations,256);
                } else {
                    key = generateKey(mainPW.toCharArray(),servID.getBytes(),iterations,512);
                }

                String encodedkey = byteArrayToHex(key.getEncoded());
                String[] splitedkey = splitStringEvery(encodedkey, 2);
                StringBuilder sb = new StringBuilder();
                for(String keypart : splitedkey) {
                    int keypart_int = Integer.parseInt(keypart,16);
                    keypart_int = keypart_int % 94;
                    keypart_int = keypart_int + 32;
                    char keypart_ascii = (char) keypart_int;
                    sb.append(keypart_ascii);
                }
                progdialog.dismiss();
                final String s = sb.toString();
                final String key_string = s.substring(0,Math.min(s.length(),length));
                MainActivity.this.runOnUiThread(new Runnable(){
                    public void run() {
                        showPW(key_string);
                    }
                });
                


            } catch (Exception e) {
                MainActivity.this.runOnUiThread(new Runnable(){
                    public void run() {
                        showAlert(getString(R.string.not_compatible), getString(R.string.not_compatible_message)); //getString(R.string.not_compatible); getString(R.string.not_compatible_message);

                    }
                });
                progdialog.dismiss();
            }


        }
    };
    new Thread(runnable).start();

}
public void showPW(final String password) {
    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
    alert.setTitle(getString(R.string.password)); //getString(R.string.password);
    alert.setMessage(password);
    alert.setPositiveButton(getString(R.string.copy2clipboard), new DialogInterface.OnClickListener() { //getString(R.string.copy2clipboard);
        public void onClick(DialogInterface dialog, int whichButton) {
            int sdk = android.os.Build.VERSION.SDK_INT;

            if(sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(password);
            } else {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
                android.content.ClipData clip = android.content.ClipData.newPlainText("",password);
                clipboard.setPrimaryClip(clip);
            }

        }
    });
    alert.setNegativeButton(getString(R.string.done), new DialogInterface.OnClickListener() { //getString(R.string.done);
      public void onClick(DialogInterface dialog, int whichButton) {

      }
  });
    alert.show();

}
}