package remix.myplayer.utils.lrc;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import remix.myplayer.infos.LrcInfo;

/**
 * Created by Remix on 2015/12/7.
 */

/**
 * 根据歌曲名和歌手名 搜索歌词并解析成固定格式
 */
public class SearchLRC {
    private static final String TAG = "SearchLRC";
    private static final String DEFAULT_LOCAL = "GB2312";
    private String mSongName;
    private String mArtistName;
    private boolean mIsFind = false;

    public SearchLRC(String musicName, String singerName) {
        //传进来的如果是汉字，那么就要进行编码转化
        try {
            mSongName = URLEncoder.encode(musicName, "utf-8");
            mArtistName = URLEncoder.encode(singerName, "utf-8");

        } catch (UnsupportedEncodingException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
    }

    /**
     * 根据歌手与歌手名,获得歌词id
     * @return 歌词id
     */
    public String getLrcUrl(){
        URL lrcIdUrl = null;
        try {
            lrcIdUrl = new URL("http://geci.me/api/lyric/" + mSongName + "/" + mArtistName);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        BufferedReader br = null;
        String s = new String();
        StringBuffer strBuffer = new StringBuffer();
        try {
            HttpURLConnection httpConn = (HttpURLConnection) lrcIdUrl.openConnection();
            httpConn.connect();
            InputStreamReader inReader = new InputStreamReader(httpConn.getInputStream());
            br = new BufferedReader(inReader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if(br == null)
                return "";
            while((s = br.readLine()) != null){
                strBuffer.append(s);
            }
            JSONObject lrcid = new JSONObject(strBuffer.toString());
            if(lrcid != null && lrcid.getInt("count") > 0 && lrcid.getInt("code") == 0){
                return lrcid.getJSONArray("result").getJSONObject(0).getString("lrc");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            try {
                if(br != null) {
                    Log.d(TAG,"StringBuffer:" + strBuffer);
                    br.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    /**
     * 根据歌词id,发送请求并解析歌词
     * @return 歌词信息list
     */
    public LinkedList<LrcInfo> getLrc(){
        //获得输入流
        BufferedReader br = null;

        //先判断该歌曲是否有缓存，没有才去下载
        try {
            DiskLruCache.Snapshot snapShot = LrcCache.getDiskCache().get(LrcCache.hashKeyForDisk(mSongName + "/" + mArtistName));
            if(snapShot != null && (br = new BufferedReader(new InputStreamReader(snapShot.getInputStream(0)))) != null ){
                return parseLrc(br,false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        //没有缓存，下载并解析歌词
        String lrcUrl = getLrcUrl();
        if(lrcUrl == null || lrcUrl.equals(""))
            return null;

        URL url = null;
        try {
            url = new URL(lrcUrl);
        } catch (MalformedURLException e2) {
            e2.printStackTrace();
        }

        try {
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            return parseLrc(br,true);
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if(br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        return null;
    }


    /**
     * 解析并缓存歌词
     * @param br 输入流
     * @param needcache 是否需要缓存
     * @return
     */
    public LinkedList<LrcInfo> parseLrc(BufferedReader br,boolean needcache){
        //解析歌词
        TreeMap<Integer,String> lrcMap = new TreeMap<>();
        lrcMap.clear();
        String s = "";
        DiskLruCache.Editor editor = null;
        OutputStream lrcCachaStream = null;
        if (br == null)
            return null;

        try {
            if (needcache) {
                editor = LrcCache.getDiskCache().edit(LrcCache.hashKeyForDisk(mSongName + "/" + mArtistName));
                lrcCachaStream = editor.newOutputStream(0);
            }
            while ((s = br.readLine()) != null) {

                if (needcache)
                    lrcCachaStream.write((s + "\r\n").getBytes());
                //判断是否是歌词内容
                if (s.startsWith("[ti") || s.startsWith("[ar") || s.startsWith("[al") ||
                        s.startsWith("[by") || s.startsWith("[off"))
                    continue;
                int startIndex = -1;
                while ((startIndex = s.indexOf("[", startIndex + 1)) != -1) {
                    int endIndex = s.indexOf("]", startIndex);
                    if (endIndex < 0)
                        continue;
                    Integer time = getMill(s.substring(startIndex, endIndex));
                    String lrc = s.substring(s.lastIndexOf(']') + 1, s.length());
                    if (time != -1 && !lrc.equals(""))
                        lrcMap.put(time, lrc);
                }
            }
            if (needcache) {
                lrcCachaStream.flush();
                editor.commit();
                LrcCache.getDiskCache().flush();
            }

        } catch (Exception e) {
            Log.d(TAG, s);
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //将解析后的歌词封装
        LinkedList<LrcInfo> list = new LinkedList();
        Iterator it = lrcMap.keySet().iterator();
        while (it.hasNext()) {
            int startime = (int)it.next();
            String sentence = lrcMap.get(startime);
            list.add(new LrcInfo(sentence,startime));
        }
        //设置每句歌词开始与结束时间
        for(int i = 0 ; i < list.size() - 1 ;i++) {
            LrcInfo cur = list.get(i);
            LrcInfo nxt = list.get(i + 1);
            list.get(i).setEndTime(nxt.getStartTime());
            list.get(i).setDuration(cur.getEndTime() - cur.getStartTime());
        }

        return list;
    }

    /**
     * 根据字符串形式的时间，得到毫秒值
     * @param strTime 时间字符串
     * @return
     */
    public int getMill(String strTime) {
        int min;
        int sec;
        int mill;
        if(strTime.substring(1,3).matches("[0-9]*"))
            min = Integer.parseInt(strTime.substring(1, 3));
        else
            return -1;
        if(strTime.substring(4,6).matches("[0-9]*"))
            sec = Integer.parseInt(strTime.substring(4, 6));
        else
            return -1;
        if(strTime.substring(7,9).matches("[0-9]*"))
            mill = Integer.parseInt(strTime.substring(7,9));
        else
            return -1;
        return min * 60000 + sec * 1000 + mill;
    }

    private void SetFindLRC(int number) {
        if(number == 0)
            mIsFind = false;
        else
            mIsFind = true;
    }

    public boolean GetFindLRC(){
        return mIsFind;
    }
}