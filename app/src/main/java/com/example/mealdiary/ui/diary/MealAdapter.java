package com.example.mealdiary.ui.diary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mealdiary.R;
import com.example.mealdiary.data.local.entity.MealRecord;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 饮食记录列表适配器
 * 用于 RecyclerView 展示三餐记录，采用卡片式布局
 * 每项显示：食物名称、餐类标签、记录时间
 */
public class MealAdapter extends RecyclerView.Adapter<MealAdapter.ViewHolder> {

    private List<MealRecord> records; // ← 数据源（从数据库查出来的列表）
    private final OnItemClickListener listener;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(MealRecord record);
    }

    public MealAdapter(List<MealRecord> records, OnItemClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    // 更新数据，刷新列表
    public void setRecords(List<MealRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    // 2. 创建每一条的"模板"（布局）
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meal_card, parent, false);
        return new ViewHolder(view);
    }

    // 3. 把数据填入模板（每条记录走一次）
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MealRecord record = records.get(position);
        holder.tvFoodName.setText(record.getFoodName() != null ? record.getFoodName() : "未知食物");

        // 餐类标签
        String mealTypeLabel;
        int typeColor;
        switch (record.getMealType() != null ? record.getMealType() : "") {
            case "breakfast": mealTypeLabel = "🥐 早餐"; typeColor = 0xFFFF9800; break;
            case "lunch":     mealTypeLabel = "🍱 午餐"; typeColor = 0xFF4CAF50; break;
            case "dinner":    mealTypeLabel = "🍽️ 晚餐"; typeColor = 0xFF2196F3; break;
            case "snack":     mealTypeLabel = "🍰 加餐"; typeColor = 0xFFE91E63; break;
            default:          mealTypeLabel = "🍽️ 餐食"; typeColor = 0xFF9E9E9E;
        }
        holder.tvMealType.setText(mealTypeLabel);
        holder.tvMealType.setTextColor(typeColor);

        // 时间
        holder.tvTime.setText(sdf.format(new Date(record.getCreateTime())));

        // 点击事件
        holder.cardView.setOnClickListener(v -> listener.onItemClick(record));
    }

    // 1. 告诉 RecyclerView 总共多少条数据
    @Override
    public int getItemCount() {
        return records != null ? records.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvFoodName, tvMealType, tvTime;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvMealType = itemView.findViewById(R.id.tvMealType);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}