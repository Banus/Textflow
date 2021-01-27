package com.example.googlemap;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Telephony;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.text.StringSubstitutor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static androidx.core.content.ContextCompat.startActivity;

class Talker {
    private TextToSpeech tts;
    public Boolean USE_FLOW = false;

    static class Rheme {
        public ArrayList<String> options = new ArrayList<>();
        public int moore_after = -1;

        public Rheme(JSONObject obj) throws JSONException { parse(obj); }

        public void parse(JSONObject obj) throws JSONException {
            JSONArray options = obj.getJSONArray("options");
            for (int j = 0; j < options.length(); ++j) this.options.add(options.getString(j));
            if (obj.has("moore_after")) moore_after = obj.getInt("moore_after");
        }
    }

    static class Task {
        public String topic;                       // topic ID
        public String description = "";            // human-understandable description of the task
        public HashMap<String, String> context;    // context for the rhemes

        public Task(JSONObject obj) throws JSONException { parse(obj); }

        public void parse(JSONObject obj) throws JSONException {
            this.topic = obj.getString("topic");
            if (obj.has("description")) {
                this.description = obj.getString("description");
            }

            this.context = new HashMap<>();
            JSONObject attr = obj.getJSONObject("attributes");
            for (Iterator<String> it = attr.keys(); it.hasNext(); ) {
                String key = it.next();
                this.context.put(key, attr.getString(key));
            }
        }
    }

    static class Scenario {
        public String description = "";
        public ArrayList<Task> tasks = new ArrayList<>();

        public Scenario(JSONObject obj) throws JSONException { parse(obj); }

        public void parse(JSONObject obj) throws JSONException {
            if (obj.has("description")) {
                this.description = obj.getString("description");
            }
            JSONArray tasks = obj.getJSONArray("tasks");
            for (int i=0; i<tasks.length(); ++i) this.tasks.add(new Task(tasks.getJSONObject(i)));
        }
    }

    private HashMap<String, Rheme> topics;
    private HashMap<String, Rheme> rhemes;
    private HashMap<String, Scenario> scenarios;

    public String curr_scenario = "scenario 1";
    private int run_id = 0;   // to stop previously running threads
    private Context context;  // for sending SMSs

    public enum Command { NONE, SELECT, NEXT, BACK, CANCEL, SYS_CANCEL }
    private Command cmd = Command.NONE;

    private HashMap<String, Rheme> _parse_rheme_list(JSONArray list) throws JSONException {
        HashMap<java.lang.String, Talker.Rheme> res = new HashMap<>();
        for (int i = 0; i < list.length(); ++i) {
            JSONObject obj = list.getJSONObject(i);
            res.put(obj.getString("name"), new Rheme(obj));
        }
        return res;
    }

    private void parse_rhemes(Context context) throws JSONException { // read JSON
        InputStream inputStream = context.getResources().openRawResource(R.raw.rhemes);
        String jsonString = new Scanner(inputStream).useDelimiter("\\A").next();
        JSONObject root = new JSONObject(jsonString);

        topics = _parse_rheme_list(root.getJSONArray("topics"));
        rhemes = _parse_rheme_list(root.getJSONArray("rhemes"));
        topics.putAll(_parse_rheme_list(root.getJSONArray("rules")));
    }

    private void parse_scenarios(Context context) throws JSONException {
        InputStream inputStream = context.getResources().openRawResource(R.raw.scenarios);
        String jsonString = new Scanner(inputStream).useDelimiter("\\A").next();
        JSONArray slist = new JSONArray(jsonString);
        scenarios = new HashMap<>();

        for (int i = 0; i < slist.length(); ++i) {
            JSONObject obj = slist.getJSONObject(i);
            scenarios.put(obj.getString("name"), new Scenario(obj));
        }
    }

    private void init_rhemes(Context context) {
        try {
            parse_rhemes(context);
            parse_scenarios(context);
        } catch (JSONException ignored) {}
        reset();
    }


    Talker(Context context) {
        // init text-to-speech (TTS)
        tts = new TextToSpeech(context, status -> {
            if(status == TextToSpeech.SUCCESS){
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("error", "This Language is not supported");
                }
            }
            else Log.e("error", "Initialization Failed!");
        });
        Log.d("TALKER", tts.getEngines().get(0).name);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        init_rhemes(context);
        this.context = context;
    }

    private Talker speak(String text) {
        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);

        if (text != null && !text.equals("")) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "test");
        }
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    private Talker silence(int ms) {
        if (ms > 0) tts.playSilentUtterance(ms, TextToSpeech.QUEUE_ADD, null);
        return this;
    }

    private Talker selected() {
        tts.setPitch(2.0f);
        tts.setSpeechRate(2.0f);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    private Talker unselected() {
        tts.setPitch(1.0f);
        tts.setSpeechRate(1.0f);
        return this;
    }

    void command(Command cmd) {
        this.cmd = cmd;
    }
    private void reset() { cmd = Command.NONE; }

    @SuppressWarnings("UnusedReturnValue")
    private Talker wait_tts() {
        // wait until finished speaking or condition is true
        do {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException ignored) {}
        } while (tts.isSpeaking());
        return this;
    }

    String resolve(String templated, HashMap<String, String> context) {
        StringSubstitutor sub = new StringSubstitutor(context);
        sub.setEnableUndefinedVariableException(true);  // exception on undefined keys
        try {
            return sub.replace(templated);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public void showSendSMS(String message)
    {
        String number = "704-318-6283";
        Intent sendIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
        sendIntent.putExtra("sms_body", message);

        String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(context);
        if (defaultSmsPackageName != null) {
            sendIntent.setPackage(defaultSmsPackageName);
        }

        try {
            startActivity(this.context, sendIntent, null);
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(context, "SMS app not available.", Toast.LENGTH_LONG).show();
        }
    }

    void run_textflow(String scenario, int task) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Scenario sc = Objects.requireNonNull(scenarios.get(scenario));
                    reset(); run_id++;
                    run_textflow(sc.tasks.get(task), run_id);
                } catch (Exception ex) {
                    Log.e("Talker", ex.toString());
                }
            }
        };
        thread.start();
    }

    void run_textflow(Task task, int run_id) {
        Rheme current = Objects.requireNonNull(topics.get(task.topic));
        HashMap<String, String> tcontext = task.context;

        boolean more = false, is_rheme = false;  // if we are checking rhemes
        int idx = 0, old_idx = -1, start = 0;
        int end = (current.moore_after > 0) ? current.moore_after + 1 : current.options.size();

        while (run_id == this.run_id) {
            if (current == null) {
                speak("Rheme not found.").wait_tts();
                break;
            }
            String prompt = (idx == current.moore_after && !more) ? "More." :
                                                                    current.options.get(idx);
            prompt = resolve(prompt, tcontext);
            if (prompt == null) {  // skip rhemes with unrelated context
                idx = (idx + 1) % (end - start) + start; continue;
            }

            if (old_idx != idx) {  // say the prompt once
                old_idx = idx;
                speak(prompt).silence(125);
            }
            // trick: wait at least 100 ms or until a command is received if not flow
            do { wait_tts(); } while(!USE_FLOW && cmd == Command.NONE);

            Log.d("Tap", String.format("cmd=%s", cmd));
            if (cmd == Command.SELECT) {
                selected().speak(prompt + ". Selected.").unselected()
                        .silence(USE_FLOW ? 1000 : 0).wait_tts();

                if (is_rheme) {
                    showSendSMS(prompt); break;  // select the message
                }
                else if (!more && idx == current.moore_after) {
                    end = current.options.size(); more = true;
                    old_idx--;  // read last entry now instead of saying "more"
                } else {  // dive into the nested list and reset state
                    current = rhemes.get(prompt);
                    is_rheme = true; start = idx = 0; old_idx = -1; more = false;
                    if (current != null) {
                        end = (current.moore_after > 0) ? current.moore_after + 1 :
                                current.options.size();
                    }
                }
            } else if (cmd == Command.CANCEL) {
                if (is_rheme) {  // return to root
                    is_rheme = false;
                    current = Objects.requireNonNull(topics.get(task.topic));
                }
                // return to main list
                more = false; start = idx = 0; old_idx = -1;
                end = (current.moore_after > 0) ? current.moore_after + 1 :
                        current.options.size();

            } else if (USE_FLOW || (cmd == Command.NEXT)) {
                idx = (idx + 1 - start) % (end - start) + start;
            } else if (cmd == Command.BACK) {
                // Java returns negatives with %
                idx = Math.floorMod(idx - 1 - start, end - start) + start;
            }
            if (cmd == Command.SYS_CANCEL) break;

            cmd = Command.NONE;  // free variable for async updates
        }
    }

    void cycle() {
        Scenario sc = Objects.requireNonNull(scenarios.get(curr_scenario));
        ArrayList<Task> tasks = sc.tasks;

        run_id = 1;
        tts.setSpeechRate(0.8f); speak(sc.description).wait_tts(); tts.setSpeechRate(1.0f);

        for (Task task : tasks) {
            tts.setSpeechRate(0.8f); speak(task.description).silence(1000).wait_tts();
            tts.setSpeechRate(1.0f);
            run_textflow(task, 1);
            reset();
            silence(5000);
        }
        run_id = 0;
    }
}
