package com.example.clfp4;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.clfp4.ListView.CheckListAdapter;
import com.example.clfp4.ListView.CheckListData;
import com.example.clfp4.Network.AppHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Daily_CheckList_Fragment extends Fragment {
    private long mNow;
    private Date mDate;
    private SimpleDateFormat mFormat = new SimpleDateFormat("yyyy년 MM월 dd일");

    private Calendar c;
    private int nYear, nMon, nDay;
    private DatePickerDialog.OnDateSetListener mDateSetListener;

    private TextView tv_date, tv_goal;
    private ImageButton btn_calendar;
    private Button btn_add;
    private EditText et_today_todo;
    private ListView listview_todolist;
    private CheckListAdapter checklistAdapter;
    private String ck_text;

    private String getEdit;
    private double count = 0.0;
    private double count_all = 0.0;

    private String strDate;

    private String url_daily = "http://192.168.35.92:8080/CheckList/Daily.jsp";
    private String url_goal = "http://192.168.35.92:8080/CheckList/Goal.jsp";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_daily_checklist, container, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        tv_date = getView().findViewById(R.id.tv_date);
        tv_goal = getView().findViewById(R.id.tv_goal);
        btn_calendar = getView().findViewById(R.id.btn_calender);
        btn_add = getView().findViewById(R.id.btn_add);
        et_today_todo = getView().findViewById(R.id.et_today_todo);
        listview_todolist = getView().findViewById(R.id.listview_todolist);
        getEdit = "";

        // 오늘 날짜 표현
        tv_date.setText(getTime(mFormat));

        //Adapter 생성
        checklistAdapter = new CheckListAdapter();

        // 리스트뷰 참조 및 Adapter달기
        listview_todolist.setAdapter(checklistAdapter);

        // request queue 는 앱이 시작되었을 때 한 번 초기화되기만 하면 계속 사용이 가능
        if(AppHelper.requestqueue == null)
            AppHelper.requestqueue = Volley.newRequestQueue(this.getActivity());

        // DB 데이터 띄우기
        todolist_SelectRequest();
        goal_SelectReques();

        // 추가 버튼 리스너
        btn_add.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                getEdit = et_today_todo.getText().toString();

                if (getEdit.getBytes().length <= 0) {
                    Toast.makeText(getContext(), "내용을 입력하세요.", Toast.LENGTH_SHORT).show();
                } else {
                    checklistAdapter.addItem(getEdit);
                    checklistAdapter.todolist_AddRequest(tv_date.getText().toString(),getEdit);

                    // listview 갱신
                    checklistAdapter.notifyDataSetChanged();
                    et_today_todo.setText("");

                    // 키보드 숨기기
                    InputMethodManager mInputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    mInputMethodManager.hideSoftInputFromWindow(et_today_todo.getWindowToken(), 0);
                }
                setGoal();
                goal_ModifyReques(setGoal());
            }
        });

        // 리스트뷰 리스너 (체크 여부)
        listview_todolist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SparseBooleanArray checkedItems = listview_todolist.getCheckedItemPositions();
                CheckListData CheckListData_temp = (CheckListData) checklistAdapter.getItem(position);
                ck_text = CheckListData_temp.getTodo();
                String ck_data = "0";
                if (checkedItems.get(position)) {
                    ck_data = "1";
                }
                todolist_CheckModifyRequest(ck_data);

                setGoal();
                goal_ModifyReques(setGoal());
            }
        });

        // 길게 누를시 삭제
        listview_todolist.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, View view,
                                           final int position, long id) {

                AlertDialog.Builder alertDlg = new AlertDialog.Builder(view.getContext());
                alertDlg.setTitle("삭제");

                // '예' 버튼이 클릭되면
                alertDlg.setPositiveButton("예", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CheckListData CheckListData_temp = (CheckListData) checklistAdapter.getItem(position);
                        String delete_text = CheckListData_temp.getTodo();

                        checklistAdapter.todolist_DeleteRequest(tv_date.getText().toString(),delete_text);

                        // 아래 method를 호출하지 않을 경우, 삭제된 item이 화면에 계속 보여진다.
                        checklistAdapter.removeItem(position);
                        checklistAdapter.notifyDataSetChanged();
                        dialog.dismiss();  // AlertDialog를 닫는다.
                    }
                });

                // '아니오' 버튼이 클릭되면
                alertDlg.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();  // AlertDialog를 닫는다.
                    }
                });

                alertDlg.setMessage("삭제하시겠습니까?");
                alertDlg.show();

                setGoal();
                goal_ModifyReques(setGoal());

                // 이벤트 처리 종료 , 여기만 리스너 적용시키고 싶으면 true , 아니면 false
                return true;
            }
        });

        // Calendar
        //DatePicker Listener
        mDateSetListener =
                new DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(DatePicker view, int year, int monthOfYear,
                                          int dayOfMonth) {

                        strDate = String.valueOf(year) + "년 ";

                        if (monthOfYear + 1 > 0 && monthOfYear + 1 < 10)
                            strDate += "0" + String.valueOf(monthOfYear + 1) + "월 ";
                        else
                            strDate += String.valueOf(monthOfYear + 1) + "월 ";

                        if (dayOfMonth > 0 && dayOfMonth < 10)
                            strDate += "0" + String.valueOf(dayOfMonth) + "일";
                        else
                            strDate += String.valueOf(dayOfMonth) + "일";

                        tv_date.setText(strDate);
                        count_all = checklistAdapter.getCount();
                        int i = 0;
                        while (i < count_all) {
                            checklistAdapter.removeItem(0);
                            checklistAdapter.notifyDataSetChanged();
                            i++;
                        }

                        // 체크리스트 목록, 달성률 띄움
                        todolist_SelectRequest();
                        goal_SelectReques();
                    }
                };

        c = Calendar.getInstance();
        nYear = c.get(Calendar.YEAR);
        nMon = c.get(Calendar.MONTH);
        nDay = c.get(Calendar.DAY_OF_MONTH);

        // 달력 아이콘 리스너
        btn_calendar.setOnClickListener(new ImageButton.OnClickListener() {
            @Override
            public void onClick(View view) {

                DatePickerDialog oDialog = new DatePickerDialog(getContext(),
                        mDateSetListener, nYear, nMon, nDay);
                oDialog.show();
            }
        });
    }

    public String getTime(SimpleDateFormat Format) {
        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return Format.format(mDate);
    }

    public String setGoal() {
        String goal = null;
        tv_goal = getView().findViewById(R.id.tv_goal);
        count = listview_todolist.getCheckedItemCount();
        count_all = checklistAdapter.getCount();

        if (count > 0.0 ) {
            double rate = Double.parseDouble(String.format("%.2f", count / count_all));
            int goal_rate = (int) (rate * 100);
            tv_goal.setText(goal_rate + " %");
            goal = goal_rate + " %";
        } else {
            tv_goal.setText("0 %");
            goal = "0 %";
        }
        Log.v("카운트 :", String.valueOf(count));

        return goal;
    }

    // 체크리스트 목록 조회
    public void todolist_SelectRequest() {

        // Request Obejct인 StringRequest 생성
        StringRequest request = new StringRequest(Request.Method.POST, url_daily,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                           if(response.equals("todoNotExist")){
                                Toast.makeText(getContext(), "오늘의 일정을 등록하세요.", Toast.LENGTH_SHORT).show();
                                Log.d("통신 메세지", "[" + response + "]"); // 서버와의 통신 결과 확인 목적
                            }
                           else if(response.equals("error")){
                               Log.d("통신 메세지", "[" + response + "]"); // 서버와의 통신 결과 확인 목적
                           }
                            else {
                                try {
                                JSONArray jarray = new JSONArray(response);
                                int size = jarray.length();
                                for (int i = 0; i < size; i++) {
                                    JSONObject jsonObject = jarray.getJSONObject(i);
                                    String save_check = jsonObject.getString("save_check");
                                    String save_text = jsonObject.getString("save_todo");

                                    if (save_check.equals("1")) {
                                        checklistAdapter.addItem(save_text);
                                        listview_todolist.setItemChecked(i, true);
                                        checklistAdapter.notifyDataSetChanged();
                                    } else {
                                        checklistAdapter.addItem(save_text);
                                        checklistAdapter.notifyDataSetChanged();
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                            Log.d("통신 에러", "[" + error.getMessage() + "]");
                            Log.v("통신 에러 이유",error.getStackTrace().toString());
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    params.put("date",tv_date.getText().toString());
                    params.put("type", "todoShow");
                    return params;
                }
            };

        request.setShouldCache(false); // 이전 결과가 있더라도 새로 요청해서 응답을 보여줌
        AppHelper.requestqueue.add(request); // request queue 에 request 객체를 넣어준다.

    }

    // 체크 수정
    public void todolist_CheckModifyRequest(final String check) {
        // Request Obejct인 StringRequest 생성
        StringRequest request = new StringRequest(Request.Method.POST, url_daily,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Log.d("통신 에러", "[" + error.getMessage() + "]");
                        Log.v("통신 에러 이유",error.getStackTrace().toString());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("date",tv_date.getText().toString());
                params.put("check", check);
                params.put("todo", ck_text);
                params.put("type", "checkModify");
                return params;
            }
        };

        request.setShouldCache(false); // 이전 결과가 있더라도 새로 요청해서 응답을 보여줌
        AppHelper.requestqueue.add(request); // request queue 에 request 객체를 넣어준다.

    }

    // goal 조회
    public void goal_SelectReques() {
        // Request Obejct인 StringRequest 생성
        StringRequest request = new StringRequest(Request.Method.POST, url_goal,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(response.equals("goalNotExist"))
                        {
                            Log.d("통신 메세지", "[" + response + "]"); // 서버와의 통신 결과 확인 목적
                        }
                        else if(response.equals("error")){
                            Log.d("통신 메세지", "[" + response + "]"); // 서버와의 통신 결과 확인 목적
                        }
                        else{
                            tv_goal.setText(response);
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("통신 에러", "[" + error.getMessage() + "]");
                        Log.v("통신 에러 이유",error.getStackTrace().toString());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("date", tv_date.getText().toString());
                params.put("type", "goalShow");
                return params;
            }
        };

        request.setShouldCache(false); // 이전 결과가 있더라도 새로 요청해서 응답을 보여줌
        AppHelper.requestqueue.add(request); // request queue 에 request 객체를 넣어준다.

    }

    // goal 수정
    public void goal_ModifyReques(final String goal) {
        // Request Obejct인 StringRequest 생성
        StringRequest request = new StringRequest(Request.Method.POST, url_goal,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(response.equals("goalModify")){
                            Log.d("통신 메세지", "[" + response + "]"); // 서버와의 통신 결과 확인 목적
                        }
                        else if(response.equals("error")){
                            Log.d("통신 메세지", "[" + response + "]"); // 서버와의 통신 결과 확인 목적
                        }
                        else
                        {

                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("통신 에러", "[" + error.getMessage() + "]");
                        Log.v("통신 에러 이유",error.getStackTrace().toString());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("date", tv_date.getText().toString());
                params.put("goal",goal);
                params.put("type", "goalModify");
                return params;
            }
        };

        request.setShouldCache(false); // 이전 결과가 있더라도 새로 요청해서 응답을 보여줌
        AppHelper.requestqueue.add(request); // request queue 에 request 객체를 넣어준다.

    }
}

