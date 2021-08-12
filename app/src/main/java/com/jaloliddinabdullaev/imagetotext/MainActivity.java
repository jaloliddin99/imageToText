package com.jaloliddinabdullaev.imagetotext;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    TextView textView2;
    EditText  textView;
    Button button, translateButton;
    FloatingActionButton fab;
    ClipboardManager clipMan;
    public static boolean userAgreementBoolean = false;
    public static boolean permission = false;
    public static int toTranslate;
    public static String fromLan, toLan;
    String s;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (permission && userAgreementBoolean) {
            userAgreement();
        }


        imageView = findViewById(R.id.imageId);
        textView = findViewById(R.id.textId);
        textView2 = findViewById(R.id.translateTextId);
        button = findViewById(R.id.button);
        translateButton = findViewById(R.id.trandlateButton);
        fab = findViewById(R.id.fab);
        textView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        textView.setRawInputType(InputType.TYPE_CLASS_TEXT);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
            permission = true;
            userAgreement();

        }

        textView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // do something, e.g. set your TextView here via .setText()
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                textView.clearFocus();
                translateButton.setVisibility(View.VISIBLE);
                translateButton.setText("Translate");
                s=textView.getText().toString();
                options();
                return true;
            }
            return false;
        });
        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence ss, int start, int count, int after) {
                translateButton.setVisibility(View.INVISIBLE);
                textView2.setText("");
                textView2.setVisibility(View.INVISIBLE);
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    public void doPress(View view) {
        //open camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 101);
        textView2.setText("");
        textView2.setVisibility(View.INVISIBLE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        FirebaseVisionImage firebaseVisionImage = null;

        if (requestCode == 101) {
            Bundle bundle = data.getExtras();

            Bitmap bitmap = (Bitmap) bundle.get("data");
            //set image in image view
            imageView.setImageBitmap(bitmap);
            firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
        }
        //process the image
        //create a FirebaseVisionImage object from a Bitmap object

        //2. get an instanse of fireBaseVision
        FirebaseVision firebaseVision = FirebaseVision.getInstance();
        //3, creaete an instance of FireBaseVisionTestRecogniser
        FirebaseVisionTextRecognizer firebaseVisionTextRecognizer = firebaseVision.getOnDeviceTextRecognizer();
        //4. create a task to process the image
        assert firebaseVisionImage != null;
        Task<FirebaseVisionText> task = firebaseVisionTextRecognizer.processImage(firebaseVisionImage);
        //5. if the task is success
        task.addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                s = firebaseVisionText.getText();
                textView.setText(s);
                button.setText("Take new Photo");
                translateButton.setVisibility(View.VISIBLE);
                options();

            }
        });
        //6. if the task is failure
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    private void identifyLanguage() {
        Log.i("The text is ", s);
        FirebaseLanguageIdentification identification = FirebaseNaturalLanguage.getInstance()
                .getLanguageIdentification();
        identification.identifyLanguage(s).addOnSuccessListener(str -> {
            if (str.equals("und")) {
                Toast.makeText(getApplicationContext(), "Not Detected", Toast.LENGTH_LONG).show();
            } else {
                Log.i("language code", str);
                getLanguageCode(str);
                if (str.equals("en")||str.equals("ar")||str.equals("ur")||str.equals("tr")
                        ||str.equals("fr")||str.equals("de")||str.equals("es")||str.equals("it")){
                    Locale locale=new Locale(str);
                    fromLan=locale.getDisplayLanguage(locale);
                }
            }
        });
    }
    private void getLanguageCode(String language) {
        int lanCode;
        switch (language) {
            case "en":
                lanCode = FirebaseTranslateLanguage.EN;
                translateText(lanCode);
                break;
            case "ar":
                lanCode = FirebaseTranslateLanguage.AR;
                translateText(lanCode);
                break;
            case "ur":
                lanCode = FirebaseTranslateLanguage.UR;
                translateText(lanCode);
                break;
            case "tr":
                lanCode = FirebaseTranslateLanguage.TR;
                translateText(lanCode);
                break;
            case "fr":
                lanCode = FirebaseTranslateLanguage.FR;
                translateText(lanCode);
                break;
            case "de":
                lanCode = FirebaseTranslateLanguage.DE;
                translateText(lanCode);
                break;
            case "es":
                lanCode=FirebaseTranslateLanguage.ES;
                translateText(lanCode);
                break;
            case "it":
                lanCode=FirebaseTranslateLanguage.IT;
                translateText(lanCode);
                break;
            default:
                Locale locale=new Locale(language);
                String nameOFLocale=locale.getDisplayLanguage(locale);
                AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Sorry, "+nameOFLocale+" language is not included in our dictionary.");
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setIcon(android.R.drawable.ic_dialog_info);
                builder.show();
                lanCode = 0;
                break;
        }
        Log.i("language code ", String.valueOf(lanCode));

    }
    private void translateText(int langCode) {

        FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder()
                //fron
                .setSourceLanguage(langCode)
                //to
                .setTargetLanguage(toTranslate)
                .build();
        FirebaseTranslator firebaseTranslator = FirebaseNaturalLanguage.getInstance()
                .getTranslator(options);

        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().build();
        firebaseTranslator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                firebaseTranslator.translate(s).addOnSuccessListener(new OnSuccessListener<String>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onSuccess(String string) {
                        textView2.setVisibility(View.VISIBLE);
                        textView2.setText(string);
                        fab.setVisibility(View.VISIBLE);
                        translateButton.setText(fromLan+" to "+toLan);
                        fab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                clipMan = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clip = ClipData.newPlainText("label", textView2.getText());
                                if (clipMan == null || clip == null) return;
                                clipMan.setPrimaryClip(clip);
                                Toast.makeText(getApplicationContext(), "copied", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

    }
    public void userAgreement() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("User agreement");
        builder.setMessage("This application requires 30 MB around to download each language contents for translation " +
                " only for the first time and please be patient, it will take a while to download.");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
            userAgreementBoolean = false;
            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.no, (dialog, which) -> {
            userAgreementBoolean = true;
            finish();
        });
        builder.setCancelable(false);
        builder.show();
    }
    public void options(){
        translateButton.setOnClickListener(v -> {

            final CharSequence[] options = {"English", "Russian", "German", "Cancel"};
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Choose!");
            builder.setItems(options, (dialog, item) -> {
                if (options[item].equals("English")) {
                    toTranslate = FirebaseTranslateLanguage.EN;
                    toLan="English";
                    identifyLanguage();
                } else if (options[item].equals("Russian")) {
                    toTranslate = FirebaseTranslateLanguage.RU;
                    toLan="Russian";
                    identifyLanguage();
                } else if (options[item].equals("German")) {
                    toTranslate = FirebaseTranslateLanguage.DE;
                    toLan="German";
                    identifyLanguage();
                } else if
                (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            });
            builder.show();

        });
    }
}