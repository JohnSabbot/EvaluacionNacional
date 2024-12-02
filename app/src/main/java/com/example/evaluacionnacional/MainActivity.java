package com.example.evaluacionnacional;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static String mqttHost = "tcp://dawnface614.cloud.shiftr.io:1883";
    private static String IdUsuario = "AppAndroid";

    private static String Topico = "Banios";
    private static String User = "dawnface614";
    private static String Pass = "H80yc2b3dcn9d3yx";

    private TextView textView;
    private MqttClient mqttClient;

    private EditText mensajeEditar;

    private EditText txtID, txtDireccion, txtDescripcion;
    private ListView lista;
    private Spinner spComuna;
    private Button botonEnvio;

    private FirebaseFirestore db;

    String[] comunas = {"Santiago Centro", "Independencia", "Estacion Central", "Quilicura", "Las Condes"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CargarListaFirestore();

        db = FirebaseFirestore.getInstance();

        txtID = findViewById(R.id.txtID);
        txtDireccion = findViewById(R.id.txtDireccion);
        txtDescripcion = findViewById(R.id.txtDescripcion);
        lista = findViewById(R.id.lista);
        spComuna = findViewById(R.id.spComuna);
        botonEnvio = findViewById(R.id.botonEnvioMensaje);
        textView = findViewById(R.id.textView);
        mensajeEditar = findViewById(R.id.editarMensaje);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, comunas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spComuna.setAdapter(adapter);

        try {
            mqttClient = new MqttClient(mqttHost, IdUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(User);
            options.setPassword(Pass.toCharArray());

            mqttClient.connect(options);

            Toast.makeText(this, "Aplicacion conectada al Servidor MQTT", Toast.LENGTH_SHORT).show();

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT", "Conexion perdida con el servidor MQTT");
                }
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    runOnUiThread(() -> textView.setText(payload));
                }
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Mensaje enviado correctamente");
                }

            });

    } catch (MqttException e) {
            e.printStackTrace();

            botonEnvio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String mensaje = mensajeEditar.getText().toString();
                    try {
                        if (mqttClient != null && mqttClient.isConnected()) {

                            mqttClient.publish(Topico, mensaje.getBytes(), 0, false);

                            textView.append("\n - "+ mensaje);
                            Toast.makeText(MainActivity.this, "Mensaje enviado correctamente", Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(MainActivity.this, "No hay conexion con el servidor MQTT", Toast.LENGTH_SHORT).show();
                        }
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            });
        }


    }


    public void enviarDatosFirestore(View view) {

        String ID = txtID.getText().toString();
        String Direccion = txtDireccion.getText().toString();
        String Descripcion = txtDescripcion.getText().toString();
        String Comuna = spComuna.getSelectedItem().toString();

        Map<String, Object> banio = new HashMap<>();
        banio.put("ID", ID);
        banio.put("Dirección", Direccion);
        banio.put("Descripción", Descripcion);
        banio.put("Comuna", Comuna);

        db.collection("Banios")
                .document(ID)
                .set(banio)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Datos enviados correctamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error al enviar los datos", Toast.LENGTH_SHORT).show();
                });

    }

    public void CargarLista(View view) {
        CargarListaFirestore();
    }

    public void CargarListaFirestore() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Banios")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> listaBanios = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String linea = "|| " + document.getString("ID") + " || " +
                                        document.getString("Dirección") + " || " +
                                        document.getString("Descripción") + " || " +
                                        document.getString("Comuna");
                                listaBanios.add(linea);
                            }
                            ArrayAdapter<String> adaptador = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, listaBanios);
                            lista.setAdapter(adaptador);
                        } else {
                            Log.e("TAG", "Error al obtener los datos de Firestore", task.getException());

                        }
                    }
                });
    }
}




