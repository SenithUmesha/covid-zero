package com.myhealthplusplus.app.SQLite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.myhealthplusplus.app.Models.Sick_Info;
import com.myhealthplusplus.app.Models.VaccineCenter;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseAdapter {

    DatabaseHelper helper;
    SQLiteDatabase db;
    List<Sick_Info> sickList = new ArrayList<>();
    List<VaccineCenter> vaccineCenterList = new ArrayList<>();

    public DatabaseAdapter(Context context) {
        helper = new DatabaseHelper(context);
        db = helper.getWritableDatabase();
    }

    public List<VaccineCenter> getAllCenters() {
        vaccineCenterList.clear();

        String[] columns = {DatabaseHelper.KEY_VC_CENTER, DatabaseHelper.KEY_VC_DISTRICT, DatabaseHelper.KEY_VC_POLICE_AREA, DatabaseHelper.KEY_VC_LAT, DatabaseHelper.KEY_VC_LON};
        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME2, columns, null, null, null, null, null, null);
        while (cursor.moveToNext()) {

            int index1 = cursor.getColumnIndex(DatabaseHelper.KEY_VC_CENTER);
            String center = cursor.getString(index1);
            int index2 = cursor.getColumnIndex(DatabaseHelper.KEY_VC_DISTRICT);
            String district = cursor.getString(index2);
            int index3 = cursor.getColumnIndex(DatabaseHelper.KEY_VC_POLICE_AREA);
            String police_area = cursor.getString(index3);
            int index6 = cursor.getColumnIndex(DatabaseHelper.KEY_VC_LAT);
            String lat = cursor.getString(index6);
            int index7 = cursor.getColumnIndex(DatabaseHelper.KEY_VC_LON);
            String lon = cursor.getString(index7);

            VaccineCenter vaccineCenter = new VaccineCenter(center, district, police_area, lat, lon);
            vaccineCenterList.add(vaccineCenter);
        }
        cursor.close();
        return vaccineCenterList;
    }

    public List<Sick_Info> getAllSickInfo() {
        sickList.clear();

        String[] columns = {DatabaseHelper.KEY_INFO_TOPIC, DatabaseHelper.KEY_INFO_SUB_TOPIC};
        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME1, columns, null, null, null, null, null, null);
        while (cursor.moveToNext()) {

            int index2 = cursor.getColumnIndex(DatabaseHelper.KEY_INFO_TOPIC);
            String topic = cursor.getString(index2);
            int index3 = cursor.getColumnIndex(DatabaseHelper.KEY_INFO_SUB_TOPIC);
            String sub_topic = cursor.getString(index3);

            Sick_Info sickRows = new Sick_Info(topic, sub_topic);
            sickList.add(sickRows);
        }
        cursor.close();
        return sickList;
    }

    private static class DatabaseHelper extends SQLiteAssetHelper {

        private static final String DATABASE_NAME = "HealthPlusPlus.db";
        private static final String TABLE_NAME1 = "sick_info";
        private static final String TABLE_NAME2 = "vaccine_centers";

        private static final int DATABASE_VERSION = 2;

        private static final String KEY_INFO_TOPIC = "info_topic";
        private static final String KEY_INFO_SUB_TOPIC = "info_sub_topic";

        private static final String KEY_VC_CENTER = "center";
        private static final String KEY_VC_DISTRICT = "district";
        private static final String KEY_VC_POLICE_AREA = "police_area";
        private static final String KEY_VC_LAT = "lat";
        private static final String KEY_VC_LON = "long";

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
    }
}
