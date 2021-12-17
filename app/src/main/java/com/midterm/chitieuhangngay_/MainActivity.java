package com.midterm.chitieuhangngay_;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView amounttv;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;

    private FirebaseAuth mAuth;
    private DatabaseReference ref;
    private String onlUserId = "";
    private ProgressDialog loader;

    private TodayItemsAdapter todayItemsAdapter;
    private List<Data> myDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Hoshi Budget");

        amounttv = findViewById(R.id.todayspending);
        fab = findViewById(R.id.fab);

        mAuth = FirebaseAuth.getInstance();
        onlUserId = mAuth.getCurrentUser().getUid();
        ref = FirebaseDatabase.getInstance().getReference().child("expenses").child(onlUserId);
        loader = new ProgressDialog(this);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addItem();
            }
        });

        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        myDataList = new ArrayList<>();
        todayItemsAdapter = new TodayItemsAdapter(MainActivity.this, myDataList);
        recyclerView.setAdapter(todayItemsAdapter);

        readItems();
    }

    private void readItems(){

        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Calendar cal = Calendar.getInstance();
        String date = dateFormat.format(cal.getTime());

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("expenses").child(onlUserId);
        Query query = reference.orderByChild("date").equalTo(date);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myDataList.clear();
                for (DataSnapshot dataSnapshot: snapshot.getChildren()){
                    Data data = dataSnapshot.getValue(Data.class);
                    myDataList.add(data);
                }

                todayItemsAdapter.notifyDataSetChanged();

                int totalAmount = 0;
                for (DataSnapshot ds : snapshot.getChildren()){
                    Map< String, Object> map = (Map<String, Object>) ds.getValue();
                    Object total = map.get("amount");
                    int pTotal = Integer.parseInt(String.valueOf(total));
                    totalAmount+= pTotal;

                    amounttv.setText("Chi tiêu tổng cộng: "+totalAmount + " VND");

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void addItem() {

        AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);

        View myView = inflater.inflate(R.layout.input_layout, null);
        myDialog.setView(myView);

        final AlertDialog dialog = myDialog.create();
        dialog.setCancelable(false);

        final Spinner itemSpinner = myView.findViewById(R.id.spinner);
        ArrayAdapter<String> itemAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.items));
        itemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        itemSpinner.setAdapter(itemAdapter);

        final EditText amount = myView.findViewById(R.id.amount);
        final EditText notes = myView.findViewById(R.id.note);
        final Button cancelbt = myView.findViewById(R.id.cancelbt);
        final Button savebt = myView.findViewById(R.id.savebt);

        savebt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mAmount = amount.getText().toString().trim();
                String mNotes = notes.getText().toString();
                String item = itemSpinner.getSelectedItem().toString();

                if(mAmount.isEmpty()){
                    amount.setError("Yêu cầu nhập số tiền !");
                    return;
                }

                if(item.equals("select item")){
                    Toast.makeText(MainActivity.this, "Chọn nhóm trống", Toast.LENGTH_SHORT).show();
                }

                else {
                    loader.setMessage("Đang thêm nhóm vào dữ liệu");
                    loader.setCanceledOnTouchOutside(false);
                    loader.show();

                    String id = ref.push().getKey();

                    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                    Calendar cal = Calendar.getInstance();
                    String date = dateFormat.format(cal.getTime());

                    Data data = new Data(item, date, id, mNotes, Integer.parseInt(mAmount));
                    ref.child(id).setValue(data).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(MainActivity.this, "Thêm thành công", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(MainActivity.this, "Thêm ghi chú thất bại", Toast.LENGTH_SHORT).show();
                            }

                            loader.dismiss();
                        }
                    });
                }
                dialog.dismiss();
            }
        });
        cancelbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return  true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.account){
            Intent intent = new Intent(MainActivity.this, AccountActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}