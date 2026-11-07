package com.dsh.randomalarm;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 从系统媒体库或用户选定的文件夹里随机挑一首本地音乐。 */
public class MusicLibrary {

    public static class Track {
        public Uri uri;
        public String title = "未知歌曲";
        public String artist = "";
        public long durationMs;
        public long albumId = -1;
    }

    private static final Random RND = new Random();

    /** 查询单个音频的显示名（用于指定歌曲）。 */
    public static String displayName(Context ctx, Uri uri) {
        try (Cursor c = ctx.getContentResolver().query(uri,
                new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String name = c.getString(0);
                if (name != null && !name.isEmpty()) return name;
            }
        } catch (Exception ignored) {
        }
        return uri.getLastPathSegment() == null ? "指定歌曲" : uri.getLastPathSegment();
    }

    /** folderUri 为空时用系统媒体库，否则从 SAF 文件夹里递归找。 */
    public static Track random(Context ctx, String folderUri) {
        if (folderUri != null && !folderUri.isEmpty()) {
            Track t = randomFromFolder(ctx, Uri.parse(folderUri));
            if (t != null) return t;
        }
        return randomFromMediaStore(ctx);
    }

    private static Track randomFromMediaStore(Context ctx) {
        ContentResolver cr = ctx.getContentResolver();
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0 AND "
                + MediaStore.Audio.Media.DURATION + ">=30000";

        Track chosen = null;
        int seen = 0;
        try (Cursor c = cr.query(collection, projection, selection, null, null)) {
            if (c != null) {
                int idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                while (c.moveToNext()) {
                    seen++;
                    if (RND.nextInt(seen) != 0) continue;
                    Track t = new Track();
                    t.uri = ContentUris.withAppendedId(collection, c.getLong(idCol));
                    String title = c.getString(titleCol);
                    if (title != null && !title.isEmpty()) t.title = title;
                    String artist = c.getString(artistCol);
                    if (artist != null && !artist.equals("<unknown>")) t.artist = artist;
                    t.durationMs = c.getLong(durCol);
                    t.albumId = c.getLong(albumCol);
                    chosen = t;
                }
            }
        } catch (Exception ignored) {
        }
        return chosen;
    }

    /** 用 DocumentsContract 递归遍历 SAF 树，收集音频文件后随机选一首。 */
    private static Track randomFromFolder(Context ctx, Uri treeUri) {
        ContentResolver cr = ctx.getContentResolver();
        String rootId;
        try {
            rootId = DocumentsContract.getTreeDocumentId(treeUri);
        } catch (Exception e) {
            return null;
        }
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(rootId);
        List<Track> tracks = new ArrayList<>();
        int visitedDirs = 0;
        while (!queue.isEmpty() && visitedDirs++ < 400) {
            String dirId = queue.poll();
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, dirId);
            try (Cursor c = cr.query(children, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE}, null, null, null)) {
                if (c == null) continue;
                while (c.moveToNext()) {
                    String id = c.getString(0);
                    String name = c.getString(1);
                    String mime = c.getString(2);
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                        queue.add(id);
                    } else if (isAudio(name, mime)) {
                        Track t = new Track();
                        t.uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id);
                        t.title = name != null ? name : "未知歌曲";
                        tracks.add(t);
                        if (tracks.size() >= 3000) break;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (tracks.isEmpty()) return null;
        return tracks.get(RND.nextInt(tracks.size()));
    }

    private static boolean isAudio(String name, String mime) {
        if (mime != null && mime.startsWith("audio/")) return true;
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.endsWith(".mp3") || n.endsWith(".flac") || n.endsWith(".m4a")
                || n.endsWith(".aac") || n.endsWith(".wav") || n.endsWith(".ogg")
                || n.endsWith(".opus") || n.endsWith(".wma");
    }

    /** 随机起播位置：跳过前 15%，也不落进最后 10%，避免一上来是静音或尾声。 */
    public static int randomStartMs(long durationMs) {
        return randomStartMs(durationMs, 15, 90);
    }

    /** 自定义区间：minPct～maxPct（占时长百分比）。 */
    public static int randomStartMs(long durationMs, int minPct, int maxPct) {
        if (durationMs <= 45_000) return 0;
        int lo = Math.max(0, Math.min(99, minPct));
        int hi = Math.max(lo + 1, Math.min(100, maxPct));
        double loMs = durationMs * lo / 100.0;
        double hiMs = durationMs * hi / 100.0;
        return (int) (loMs + RND.nextDouble() * (hiMs - loMs));
    }
}
