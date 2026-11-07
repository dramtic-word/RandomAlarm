package com.dsh.randomalarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

/** 数值 Spinner：预设值 + “自定义…”输入。 */
public class NumberSpinner {
    public interface OnSave {
        void save(int value);
    }

    private interface Cancel {
        void run();
    }

    public static void bind(Activity act, Spinner sp, int[] presets, String unit,
                            int min, int max, int current, OnSave onSave) {
        if (sp == null) return;
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        for (int p : presets) {
            labels.add(p + " " + unit);
            values.add(p);
        }
        int sel = values.indexOf(current);
        if (sel < 0) {
            labels.add("当前：" + current + " " + unit);
            values.add(current);
            sel = values.size() - 1;
        }
        labels.add("自定义…");
        ArrayAdapter<String> ad = new ArrayAdapter<>(act,
                android.R.layout.simple_spinner_item, labels);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        final boolean[] init = {true};
        sp.setSelection(sel, false);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (init[0]) {
                    init[0] = false;
                    return;
                }
                if (position == labels.size() - 1) {
                    showDialog(act, unit, min, max, current, v -> {
                        onSave.save(v);
                        bind(act, sp, presets, unit, min, max, v, onSave);
                    }, () -> bind(act, sp, presets, unit, min, max, current, onSave));
                } else {
                    onSave.save(values.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private static void showDialog(Activity act, String unit, int min, int max, int current,
                                   OnSave ok, Cancel cancel) {
        EditText et = new EditText(act);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setHint(min + "~" + max);
        et.setText(String.valueOf(current));
        new AlertDialog.Builder(act)
                .setTitle("自定义（" + unit + "）")
                .setView(et)
                .setPositiveButton("确定", (d, w) -> {
                    int v;
                    try {
                        v = Integer.parseInt(et.getText().toString().trim());
                    } catch (Exception e) {
                        v = current;
                    }
                    v = Math.max(min, Math.min(max, v));
                    ok.save(v);
                })
                .setNegativeButton("取消", (d, w) -> cancel.run())
                .setOnCancelListener(d -> cancel.run())
                .show();
    }
}
