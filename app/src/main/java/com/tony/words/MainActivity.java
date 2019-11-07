// Insert your own package statement 
// e.g
package com.tony.words;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;
import com.tony.words.R;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

import static android.view.View.GONE;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DICTIONARY";



    Button searchButton;
    EditText enterWord;
    TextView wordDefinition;
    ImageView wordImage;
    TextView wordPronunciation;
    ProgressBar loading;

    WordService wordService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://owlbot.info/api/v3/dictionary/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        //creating new retro fit instance with base url and converter for JSON to objects

        wordService = retrofit.create(WordService.class);


        searchButton = findViewById(R.id.search_button);
        enterWord = findViewById(R.id.enter_word);
        wordDefinition = findViewById(R.id.word_definition);
        wordImage = findViewById(R.id.word_image);
        wordPronunciation = findViewById(R.id.text_pronunciation);
        loading = findViewById(R.id.progressBar);

        setSearchBarEnabled(true);

        // Hide ImageView until an image is available 
        wordImage.setVisibility(GONE);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String word = enterWord.getText().toString();
                if (word.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Enter a word", Toast.LENGTH_SHORT).show();
                    return;
                }

                hideKeyboard();
                setSearchBarEnabled(false);
                getDefinitionForWord(word);
            }
        });
    }

    private void getDefinitionForWord(final String word) {

        wordService.getDefinition(word, BuildConfig.OWLBOT_TOKEN).enqueue(new Callback<Word>() {
            @Override
            public void onResponse(Call<Word> call, Response<Word> response) {

                setSearchBarEnabled(true);

                Word wordResponse = response.body();
                Log.d(TAG, "Word response: " + wordResponse);

                if (wordResponse != null && wordResponse.definitions.length >= 1){
                    wordDefinition.setText(wordResponse.definitions[0].definition);

                    String pronunciation = wordResponse.pronunciation;

                    if (pronunciation == null || pronunciation.isEmpty()) {
                        wordPronunciation.setText("No pronunciation found");
                    }else {
                        wordPronunciation.setText("pronunciation: " + pronunciation);
                    }
                    String imageURL = wordResponse.definitions[0].image_url;
                    if (imageURL == null || imageURL.isEmpty()) {
                        wordImage.setVisibility(GONE); // hide if no image available
                    }else  {
                        wordImage.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Image url is " +  imageURL);
                        Picasso.get().load(imageURL).fit().centerCrop().into(wordImage);
                    }
                }else {
                    Log.d(TAG, "Search for " + word + " did not return any definitions");
                    Toast.makeText(MainActivity.this, "No definitions found for " + word, Toast.LENGTH_LONG).show();

                }
            }

            @Override
            public void onFailure(Call<Word> call, Throwable t) {

                setSearchBarEnabled(false);

                Log.e(TAG, "Error fetching definition", t);
                Toast.makeText(MainActivity.this, "Unable to fetch definition", Toast.LENGTH_LONG).show();

            }
        });


        // todo
    }

    private void hideKeyboard() {
        View mainView = findViewById(android.R.id.content);
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(mainView.getWindowToken(), 0);
    }
    private void setSearchBarEnabled(boolean isEnabled) {
        loading.setVisibility( isEnabled ? GONE : View.VISIBLE);
        searchButton.setEnabled(isEnabled);
        enterWord.setEnabled(isEnabled);
    }

    public class Definition {
        String definition;
        String image_url;
        String example;
        String type;


        @Override
        public String toString() {
            return "Definition{" +
                    "definition='" + definition + '\'' +
                    ", image_url='" + image_url + '\'' +
                    ", example='" + example + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }

    class Word {
        String word;
        String pronunciation;
        Definition[] definitions;

        @Override
        public String toString() {
            return "Word{" +
                    "word='" + word + '\'' +
                    ", pronunciation='" + pronunciation + '\'' +
                    ", definitions=" + Arrays.toString(definitions) +
                    '}';
        }
    }

    public interface WordService {
        @GET("{word}")
        Call<Word> getDefinition(@Path("word") String word, @Header("Authorization") String token);
    }

}