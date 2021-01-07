package ua.com.programmer.simpleremote;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Locale;

import ua.com.programmer.simpleremote.settings.Constants;

public class Utils {

    // cleans logs text for easy reading JSON structure
    private String cleanMessage(String message){
        message = message.replace("\"{","{");
        message = message.replace("}\"","}");
        message = message.replace("\\","");
        return message;
    }

    public void log(String logType, String message){
        message = cleanMessage(message);
        switch (logType){
            case "i":
                Log.i("XBUG",message);
                break;
            case "e":
                Log.e("XBUG",message);
                break;
            default:
                Log.w("XBUG",message);
                break;
        }
    }

    public void log(String message){
        message = cleanMessage(message);
        Log.i("XBUG",message);
    }

    StringBuilder readLogs() {
        StringBuilder logBuilder = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("logcat -d XBUG:D *:E");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            int linesCounter = 0;
            while ((line = bufferedReader.readLine()) != null) {
                line = line + "\n";
                logBuilder.append(line);
                linesCounter++;
                if (linesCounter>=1000){
                    logBuilder.append("... Output size limit reached");
                    break;
                }
            }
        } catch (IOException e) {
            String error = "Error reading logs: "+e.toString();
            logBuilder.append(error);
        }
        return logBuilder;
    }

    int getPageTitleID(String tag){
        switch (tag){
            case Constants.GOODS: return R.string.header_goods_list;
            case Constants.SHIP: return R.string.document_title_shipment;
            case Constants.RECEIVE: return R.string.document_title_receive;
            case Constants.INVENTORY: return R.string.header_inventory_list;
            case Constants.DOCUMENTS: return R.string.header_documents;
            case Constants.CATALOGS: return R.string.header_catalogs;
            default: return R.string.app_name;
        }
    }

    public String currentDate(){
        Date currentDate = new Date();
        //long time = Integer.parseInt(String.format("%ts",currentDate));
        //return String.format(Locale.getDefault(),"%1$td-%1$tm-%1$tY %1$tH:%1$tM",currentDate);
        return String.format(Locale.getDefault(),"%1$td.%1$tm.%1$tY",currentDate);
    }

    String format (double i, int accuracy) {
        return String.format(Locale.getDefault(),"%."+accuracy+"f",i).replace(",",".");
    }

    String formatAsInteger (double i) {
        if (i == round(i,0)) {
            return ""+((int) i);
        }
        return format(i,3);
    }

    String formatAsInteger (String i) {
        double number = round(i,3);
        return formatAsInteger(number);
    }

    double round (double i, int accuracy) {
        return Double.parseDouble(format(i,accuracy));
    }

    double round (String i, int accuracy) {
        double result=0;
        //need to use try{} for cases when input string contains letters
        try {
            result = round(Double.parseDouble(i), accuracy);
        }catch (Exception ex){
            //log("w","round String: "+ex.toString());
        }
        return result;
    }

}
