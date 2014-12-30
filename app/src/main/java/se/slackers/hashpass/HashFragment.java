package se.slackers.hashpass;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import se.slackers.hashpass.generator.Generator;

public class HashFragment extends Fragment {
    private static final String DOMAINS = "domains";
    private static final String PASSWORD = "password";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_hash, container, false);

        // input fields
        final EditText passwordView = (EditText)rootView.findViewById(R.id.masterPassword);
        final AutoCompleteTextView saltView = (AutoCompleteTextView)rootView.findViewById(R.id.saltText);
        final EditText generatedView = (EditText)rootView.findViewById(R.id.generatedPassword);

        // buttons
        final Button generateButton = (Button)rootView.findViewById(R.id.generateButton);
        final Button copyButton = (Button)rootView.findViewById(R.id.copyButton);

        // populate
        loadSuggestions(saltView);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (preferences.getBoolean("save_master_password", false)) {
            passwordView.setText(preferences.getString(PASSWORD, ""));
        }

        // set actions
        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String salt = saltView.getText().toString().toLowerCase().trim();
                final String password = passwordView.getText().toString();

                addSuggestion(salt);
                savePassword(password);
                saltView.setText(salt);
                new GenerateTask(getActivity(), salt, password, generatedView).execute(generateButton, copyButton);
            }
        });

        copyButton.setEnabled(false);
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String password = generatedView.getText().toString().trim();
                final ClipData clip = ClipData.newPlainText("generated password", password);

                final ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(clip);

                passwordView.setText("");

                getActivity().finish();
            }
        });

        return rootView;
    }

    private void savePassword(String password) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String value = preferences.getBoolean("save_master_password", false) ? password : "";

        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PASSWORD, value);
        editor.commit();
    }

    private void addSuggestion(String domain) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final Set<String> alternatives = preferences.getStringSet(DOMAINS, new HashSet<String>());
        if (alternatives.size() >= 100) {
            alternatives.iterator().remove();
        }
        alternatives.add(domain);

        final SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(DOMAINS, alternatives);
        editor.commit();
    }

    private void loadSuggestions(AutoCompleteTextView saltView) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final Set<String> alternatives = preferences.getStringSet(DOMAINS, new HashSet<String>());

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>(alternatives));
        saltView.setAdapter(adapter);
    }

    private class GenerateTask extends AsyncTask<View, Void, String> {
        private ProgressDialog dialog;
        private String salt;
        private String password;
        private EditText resultView;

        public GenerateTask(Context context, String salt, String password, EditText resultView) {
            this.salt = salt;
            this.password = password;
            this.resultView = resultView;

            this.dialog = new ProgressDialog(context);
            this.dialog.setMessage(context.getString(R.string.generating_hash_message));
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (result == null) {
                Toast.makeText(getActivity(), R.string.generating_error_message, Toast.LENGTH_LONG).show();
                return;
            }

            resultView.setText(result);
        }

        @Override
        protected String doInBackground(final View... views) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    for (View view : views) {
                        view.setEnabled(false);
                    }
                }
            });

            try {
                return Generator.generate(salt, password);
            } catch (Exception e) {
                Log.e(Config.LOG, "Error generating password: " + e.getMessage());
                return null;
            } finally {

                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        for (View view : views) {
                            view.setEnabled(true);
                        }
                    }
                });

            }
        }
    }
}
