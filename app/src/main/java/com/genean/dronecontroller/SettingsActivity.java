package com.genean.dronecontroller;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.genean.dronecontroller.packet.Packet;
import com.genean.dronecontroller.packet.PacketHandler;

public class SettingsActivity extends AppCompatActivity {
    private float proportional;
    private float integral;
    private float derivative;

    private EditText proportionalField;
    private EditText integralField;
    private EditText derivativeField;
    private Spinner yprSpinner;
    private Axis selectedAxis;

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
        ArrayAdapter<Axis> adapter = new ArrayAdapter<>(this, R.layout.activity_settings, Axis.values());
        yprSpinner.setAdapter(adapter);
    }

    private void initSpinnerEventListener() {
        yprSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAxis = (Axis) parent.getItemAtPosition(position);
                queryPIDValuesFromHost();
                resetInputFields();
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
        Packet result = new Packet(FlightCommand.HOST_PID_RESPONSE);

        result.setOnCompleted(response -> {
            if (response == Packet.Response.TIMEOUT) {
                toast(R.string.packet_response_timeout);
            } else if (response == Packet.Response.IO_ERROR) {
                toast(R.string.packet_response_fail);
            } else if (response == Packet.Response.SUCCESS) {
                byte[] data = result.getData();
                String s = new String(data);

                if (!s.matches("((P|I|D)\\d\\.\\d{3}){3}")) {
                    toast(R.string.packet_response_corrupted);
                    return;
                }

                parseAndUpdatePIDValues(s);
                toast(R.string.pid_query_success);
            }
        });

        PacketHandler.INSTANCE.receiveHostPacketResponse(result, false);
    }

    private void parseAndUpdatePIDValues(String data) {
        String[] values = data.split("[PID]");
        this.proportional = Float.parseFloat(values[1]);
        this.integral = Float.parseFloat(values[2]);
        this.derivative = Float.parseFloat(values[3]);
    }

    private void resetInputFields() {
        proportionalField.setText(String.valueOf(proportional));
        integralField.setText(String.valueOf(integral));
        derivativeField.setText(String.valueOf(derivative));
    }

    private void onUpdateClicked(View view) {
        updateCurrentPIDValuesFromUserInput();
        updateCurrentPIDValuesToHost();
    }

    private void updateCurrentPIDValuesFromUserInput() {
        proportional = Float.parseFloat(proportionalField.getText().toString());
        integral = Float.parseFloat(integralField.getText().toString());
        derivative = Float.parseFloat(derivativeField.getText().toString());
    }

    private void updateCurrentPIDValuesToHost() {
        String data = String.format(FlightCommand.CLIENT_PID_UPDATE.toString(), selectedAxis.getSymbol(),
                proportional, integral, derivative);
        Packet packet = new Packet(data.getBytes());
        packet.setOnCompleted(response -> {
            if (response == Packet.Response.SUCCESS) {
                waitForHostToAcknowledgeUpdate();
            } else {
                toast(R.string.packet_send_fail);
            }
        });
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
