package com.example.coolweather;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.coolweather.db.City;
import com.example.coolweather.db.County;
import com.example.coolweather.db.Province;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by 彭旎 on 2017/7/6.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;


    /*ListView*/
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();

    /*省、市、县列表*/
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    /*选择的省、市*/
    private Province selectedProvince;
    private City selectedCity;

    /*当前选中的级别*/
    private int currentLevel;

    /*碎片必须重载的方法*/

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        /*动态加载布局*/
        View view = inflater.inflate(R.layout.choose_area,container,false);
        /*获取布局中的控件*/
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);

        /*设置ListView*/
        adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        /*设置ListView监听*/
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(currentLevel == LEVEL_PROVINCE)
                {
                    selectedProvince = provinceList.get(i);
                    queryCities();
                }else  if(currentLevel == LEVEL_CITY)
                {
                    selectedCity = cityList.get(i);
                    queryCounties();
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(currentLevel == LEVEL_COUNTY)
                {
                    /*当前列表是县，，则去查询市并显示*/
                    queryCities();
                }else if(currentLevel == LEVEL_CITY)
                {
                    /*当前列表是市，则出查询省*/
                    queryProvinces();
                }
            }
        });

        /*创建活动时查询省*/
        queryProvinces();
    }

    /*
    * 查询选中省内所有的省，优先从数据库查询，没有再去服务器查询
    * */
    private void queryProvinces()
    {

        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        /*若在数据库中查询到*/
        if(provinceList.size() > 0)
        {
            dataList.clear();
            /*遍历表*/
            for(Province province : provinceList)
            {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }
        else
        {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }


    /**
     * 查询省内的所有市
     * */
    private void queryCities()
    {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        /*查询该省里的所有市*/
        cityList = DataSupport.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size() > 0)
        {
            dataList.clear();
            for(City city : cityList)
            {
                dataList.add(city.getCityName());
            }
            /*重新加载listView*/
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;

        }
        else
        {
            /*从服务器查询省内所有市*/
            int provincdeCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provincdeCode;
            Log.d("test","地址为:"+address);
            this.queryFromServer(address,"city");
        }
    }

    /**
    * 查询市内所有的县，先从数据库再到服务器
    * */
    private void queryCounties()
    {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size() > 0)
        {
            dataList.clear();
            for(County county : countyList)
            {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }
        else
        {
            /*服务器查询市下所有的县*/
            int provinceCoede = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCoede + "/"+cityCode;
            Log.d("test","服务器开始去获取");
            queryFromServer(address,"county");
        }
    }


    /**
     * 从服务器查询省市县数据,查询后将结果解析，解析后存入数据库在继续查询数据库
     * */
    public void queryFromServer(String address,final String type)
    {
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                /*回到主线程*/
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = true;
                if ("province".equals(type))
                {
                    result = Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type))
                {
                    Log.d("test","开始解析数据");
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());

                }
                else if("county".equals(type))
                {
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }

                if(result)
                {
                    /*切换到主线程UI操作*/
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type))
                            {
                                queryProvinces();
                            }else if("city".equals(type))
                            {
                                queryCities();
                            }else if("county".endsWith(type))
                            {
                                queryCounties();
                            }
                        }
                    });
                }
                else
                {
                    Toast.makeText(getContext(),"获取服务器数据失败",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /*
    * 显示进度条
    * */
    public void showProgressDialog()
    {
        if(progressDialog == null)
        {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度条
     * */
    public void closeProgressDialog()
    {
        if(progressDialog != null)
        {
            progressDialog.dismiss();
        }
    }

}

