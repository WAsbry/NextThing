package com.wasbry.nextthing.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wasbry.nextthing.R
import com.wasbry.nextthing.database.model.TodoTask

class TodoTaskAdapter(private val tasks: List<TodoTask>) :
    RecyclerView.Adapter<TodoTaskAdapter.TodoTaskViewHolder>() {

    // 点击事件监听器接口
    interface OnTaskClickListener {
        fun onTaskClick(task: TodoTask)
    }

    private var clickListener: OnTaskClickListener? = null

    fun setOnTaskClickListener(listener: OnTaskClickListener) {
        clickListener = listener
    }

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoTaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo_task, parent, false)
        return TodoTaskViewHolder(view)
    }

    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: TodoTaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.title.text = task.title
        holder.description.text = task.description
        holder.completed.isChecked = task.isCompleted

        holder.itemView.setOnClickListener {
            clickListener?.onTaskClick(task)
        }
    }

    // 获取数据项数量
    override fun getItemCount(): Int {
        return tasks.size
    }

    // ViewHolder 类
    inner class TodoTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.task_title)
        val description: TextView = itemView.findViewById(R.id.task_description)
        val completed: CheckBox = itemView.findViewById(R.id.task_completed)
    }
}