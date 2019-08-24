package com.genean.dronecontroller;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.genean.dronecontroller.packet.Packet;
import com.genean.dronecontroller.packet.PacketHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    private float proportional;
    private float integral;
    private float derivative;

    private EditText proportionalField;
    private EditText integralField;
    private EditText derivativeField;
    private Spinner yprSpinner;
    private Axis selectedAxis;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> task = CompletableFuture.completedFuture(null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        this.proportionalField = findViewById(R.id.pField);
        this.integralField = findViewById(R.id.iField);
        this.derivativeField = findViewById(R.id.dField);

        final Button updateButton = findViewById(R.id.updateButton);
        updateButton.setOnClickListener(this::onUpdateClicked);
        final Button resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(this::onResetClicked);

        yprSpinner = findViewById(R.id.yprSpinner);
        initSpinnerItems();
        initSpinnerEventListener();

    }

    private void initSpinnerItems() {
        ArrayAdapter<Axis> adapter = new ArrayAdapter<>(this, R.layout.spinner_ypr, Axis.values());
        yprSpinner.setAdapter(adapter);
    }

    private void initSpinnerEventListener() {
        yprSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAxis = (Axis) parent.getItemAtPosition(position);

                if (!task.isDone())
                    return;
                task = executor.submit(SettingsActivity.this::queryPIDValuesFromHost);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                toast(R.string.error_nothing_selected);
            }
        });
    }


    private void queryPIDValuesFromHost() {
        String data = String.format(FlightCommand.CLIENT_PID_QUERY.toString(), selectedAxis.getSymbol());
        Packet query = new Packet(data.getBytes());

        query.setOnCompleted(response -> {
            if (response == Packet.Response.SUCCESS) {
                waitForHostResponse();
            } else {
                toast(R.string.packet_send_fail);
            }
        });

        PacketHandler.INSTANCE.sendPacketToHost(query);
    }

    private void waitForHostResponse() {
        Packet result = new Packet(new byte[24]);

        result.setOnCompleted(response -> {
            if (response == Packet.Response.TIMEOUT) {
                toast(R.string.packet_response_timeout);
            } else if (response == Packet.Response.IO_ERROR) {
                toast(R.string.packet_response_fail);
            } else if (response == Packet.Response.SUCCESS) {
                byte[] data = result.getData();
                String s = new String(data);

                Log.i(TAG, s);

                //P000.000I000.000D000.000
                if (!s.matches("((P|I|D) {0,2}\\d{1,3}\\.\\d{3}){3}")) {
                    toast(R.string.packet_response_corrupted);
                    return;
                }

                parseAndUpdatePIDValues(s);
                resetInputFields();
                toast(R.string.pid_query_success);
            }
        });

        PacketHandler.INSTANCE.receiveHostPacketResponse(result, false);
    }

    private void parseAndUpdatePIDValues(String data) {
        String[] values = data.split("[PID]");
        Log.i("sdf", values[1]);
        this.proportional = Float.parseFloat(values[1].trim());
        this.integral = Float.parseFloat(values[2].trim());
        this.derivative = Float.parseFloat(values[3].trim());
    }

    private void resetInputFields() {
        proportionalField.setText(String.valueOf(proportional));
        integralField.setText(String.valueOf(integral));
        derivativeField.setText(String.valueOf(derivative));
    }

    private void onUpdateClicked(View view) {
        if (!task.isDone())
            return;

        task = executor.submit(() -> {
            updateCurrentPIDValuesFromUserInput();
            updateCurrentPIDValuesToHost();
        });
    }

    private void updateCurrentPIDValuesFromUserInput() {
        Log.i("sf", proportionalField.getText().toString());
        Log.i("sf", Float.parseFloat(proportionalField.getText().toString()) + "");
        proportional = Float.parseFloat(proportionalField.getText().toString());
        integral = Float.parseFloat(integralField.getText().toString());
        derivative = Float.parseFloat(derivativeField.getText().toString());
    }

    private void updateCurrentPIDValuesToHost() {
        String data = String.format(FlightCommand.CLIENT_PID_UPDATE.toString(), selectedAxis.getSymbol(),
                proportional, integral, derivative);
        Log.i(TAG, data);
        Packet packet = new Packet(data.getBytes());
        packet.setOnCompleted(response -> {
            if (response == Packet.Response.SUCCESS) {
                waitForHostToAcknowledgeUpdate();
            } else {
                toast(R.string.packet_send_fail);
            }
        });

        PacketHandler.INSTANCE.sendPacketToHost(packet);
    }


    private void waitForHostToAcknowledgeUpdate() {
        Packet result = new Packet(FlightCommand.HOST_PID_UPDATE_RESPONSE);
        result.setOnCompleted(response -> {
            if (response == Packet.Response.TIMEOUT) {
                toast(R.string.packet_response_timeout);
            } else if (response == Packet.Response.IO_ERROR) {
                toast(R.string.packet_response_fail);
            } else if (response == Packet.Response.CORRUPTED_DATA) {
                toast(R.string.packet_response_corrupted);
            } else if (response == Packet.Response.SUCCESS) {
                toast(R.string.pid_update_success);
                Intent intent = new Intent(this, ConnectActivity.class);
                this.startActivity(intent);
            }
        });

        PacketHandler.INSTANCE.receiveHostPacketResponse(result, true);
    }

    private void onResetClicked(View view) {
        resetInputFields();
    }

    private void toast(int id) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), getString(id), Toast.LENGTH_LONG).show());
    }
}
