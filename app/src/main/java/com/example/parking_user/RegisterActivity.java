package com.example.parking_user;

import android.content.Intent;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private EditText et_id, et_password, et_name, et_role, et_age, et_gender, et_phone_number;
    private Button btn_register, btn_exam;
    private AlertDialog dialog;
    private boolean exam = false; //중복검사 여부

    @Override
    protected void onCreate(Bundle savedInstanceState) { // 액티비티 시작시 처음으로 실행되는 생명주기!
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 아이디 값 찾아주기
        et_id = (EditText) findViewById(R.id.et_id);
        et_password = (EditText) findViewById(R.id.et_password);
        et_name = (EditText) findViewById(R.id.et_name);
        et_role = (EditText) findViewById(R.id.et_role);
        et_age = (EditText) findViewById(R.id.et_age);
        et_gender = (EditText) findViewById(R.id.et_gender);
        et_phone_number = (EditText) findViewById(R.id.et_phone_number);


        // 아이디 중복검사 버튼 클릭 시 수행
        btn_exam = (Button) findViewById(R.id.btn_exam);
        btn_exam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // EditText에 현재 입력되어있는 값을 get(가져온다)해온다.
                String id=et_id.getText().toString();
                if(exam)
                {
                    return;
                }
                if(id.equals("")){
                    AlertDialog.Builder builder=new AlertDialog.Builder( RegisterActivity.this );
                    dialog=builder.setMessage("아이디는 빈 칸일 수 없습니다")
                            .setPositiveButton("확인",null)
                            .create();
                    dialog.show();
                    return;
                }
                Response.Listener<String> responseListener=new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonResponse=new JSONObject(response);
                            boolean success=jsonResponse.getBoolean("success");
                            if(success){
                                AlertDialog.Builder builder=new AlertDialog.Builder( RegisterActivity.this );
                                dialog=builder.setMessage("사용할 수 있는 아이디입니다.")
                                        .setPositiveButton("확인",null)
                                        .create();
                                dialog.show();
                                et_id.setEnabled(false);
                                exam=true;
                                btn_exam.setText("확인");
                            }
                            else{
                                AlertDialog.Builder builder=new AlertDialog.Builder( RegisterActivity.this );
                                dialog=builder.setMessage("사용할 수 없는 아이디입니다.")
                                        .setNegativeButton("확인",null)
                                        .create();
                                dialog.show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
                ExamRequest examRequest=new ExamRequest(id,responseListener);
                RequestQueue queue= Volley.newRequestQueue(RegisterActivity.this);
                queue.add(examRequest);

            }
        });


        // 회원가입 버튼 클릭 시 수행
        btn_register = (Button) findViewById(R.id.btn_register);
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // EditText에 현재 입력되어있는 값을 get(가져온다)해온다.
                String id = et_id.getText().toString();
                String password = et_password.getText().toString();
                String name = et_name.getText().toString();
                String role = et_role.getText().toString();
                String age = et_age.getText().toString();
                String gender = et_gender.getText().toString();
                String phone_number = et_phone_number.getText().toString();


                Response.Listener<String> responseListener = new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            boolean success = jsonObject.getBoolean("success");
                            if (success&&exam) { // 회원등록에 성공한 경우
                                Toast.makeText(getApplicationContext(),"회원 등록에 성공하였습니다.",Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                startActivity(intent);
                            }else if(exam == false){ //아이디 등록이 안된 경우(중복인 경우)
                                Toast.makeText(getApplicationContext(),"아이디 중복 확인이 필요합니다.",Toast.LENGTH_SHORT).show();
                                return;
                            } else { // 회원등록에 실패한 경우
                                Toast.makeText(getApplicationContext(),"회원 등록에 실패하였습니다.",Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                };
                // 서버로 Volley를 이용해서 요청을 함.
                RegisterRequest registerRequest = new RegisterRequest(id,password,name,role,age,gender,phone_number,responseListener);
                RequestQueue queue = Volley.newRequestQueue(RegisterActivity.this);
                queue.add(registerRequest);

            }
        });

    }
}
