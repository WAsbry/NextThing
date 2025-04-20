package com.wasbry.nextthing.navigation

// 导入用于绘制边框的类
import androidx.compose.foundation.BorderStroke
// 导入用于显示图片的组件
import androidx.compose.foundation.Image
// 导入用于设置背景的修饰符
import androidx.compose.foundation.background
// 导入用于添加点击事件的修饰符
import androidx.compose.foundation.clickable
// 导入用于垂直和水平排列子元素的布局组件
import androidx.compose.foundation.layout.Arrangement
// 导入用于创建容器的组件
import androidx.compose.foundation.layout.Box
// 导入用于垂直排列子元素的布局组件
import androidx.compose.foundation.layout.Column
// 导入用于添加间隔的组件
import androidx.compose.foundation.layout.Spacer
// 导入用于填充宽度的修饰符
import androidx.compose.foundation.layout.fillMaxWidth
// 导入用于设置高度的修饰符
import androidx.compose.foundation.layout.height
// 导入用于设置内边距的修饰符
import androidx.compose.foundation.layout.padding
// 导入用于设置大小的修饰符
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
// 导入用于创建圆角形状的类
import androidx.compose.foundation.shape.RoundedCornerShape
// 导入 Material Design 3 的卡片组件
import androidx.compose.material3.Card
// 导入 Material Design 3 卡片的默认设置类
import androidx.compose.material3.CardDefaults
// 导入用于显示文本的组件
import androidx.compose.material3.Text
// 导入用于标记可组合函数的注解
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
// 导入用于设置修饰符的类
import androidx.compose.ui.Modifier
// 导入用于添加阴影的修饰符
import androidx.compose.ui.draw.shadow
// 导入用于表示颜色的类
import androidx.compose.ui.graphics.Color
// 导入用于绘制图像的类
import androidx.compose.ui.graphics.painter.Painter
// 导入用于从资源加载图像的函数
import androidx.compose.ui.res.painterResource
// 导入用于设置图像内容缩放方式的枚举类
import androidx.compose.ui.layout.ContentScale
// 导入用于设置文本样式的类
import androidx.compose.ui.text.TextStyle
// 导入用于设置字体粗细的枚举类
import androidx.compose.ui.text.font.FontWeight
// 导入用于表示尺寸的单位类
import androidx.compose.ui.unit.Dp
// 导入用于表示尺寸的单位类（dp 单位）
import androidx.compose.ui.unit.dp
// 导入用于表示字体大小的单位类（sp 单位）
import androidx.compose.ui.unit.sp

// 自定义底部导航栏图标组件
@Composable
fun CustomBottomNavIcon(
    modifier: Modifier = Modifier, // 新增 Modifier 参数
    // 图标资源 ID，用于从资源中加载图标
    iconResId: Int,
    // 图标下方的说明文字
    text: String,
    // 是否选中状态
    isSelected: Boolean,
    // 选中时的颜色
    selectedColor: Color,
    // 未选中时的颜色
    unselectedColor: Color,
    // 点击事件回调
    onClick: () -> Unit
) {
    // 根据选中状态设置背景颜色，选中时为选中颜色的半透明色，未选中时为透明色
    val backgroundColor = if (isSelected) selectedColor.copy(alpha = 0.2f) else Color.Transparent
    // 根据选中状态设置内容颜色，选中时为选中颜色，未选中时为未选中颜色
    val contentColor = if (isSelected) selectedColor else unselectedColor

    // 创建一个卡片组件，作为整个图标的容器
    Card(
        // 设置卡片的修饰符，填充宽度、设置高度、添加内边距和点击事件
        modifier = Modifier
            .width(73.dp) // 设置 Card 宽度为屏幕宽度的五分之一
            .height(66.dp)
            .padding(4.dp)
            .clickable { onClick() },
        // 设置卡片的形状为圆角矩形
        shape = RoundedCornerShape(12.dp),
        // 设置卡片的颜色，根据选中状态设置背景颜色
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        // 设置卡片的边框，选中时为选中颜色的边框，未选中时无边框
        border = BorderStroke(1.dp, if (isSelected) selectedColor else Color.Transparent),
        // 设置卡片的阴影，选中时显示阴影，未选中时无阴影
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 0.dp
        )
    ) {
        // 在卡片内创建一个垂直排列的布局
        Column(
            // 设置布局的修饰符，填充宽度、添加内边距
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            // 设置子元素的垂直排列方式为居中
            verticalArrangement = Arrangement.Center,
            // 设置子元素的水平排列方式为居中
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 从资源中加载图标
            val iconPainter: Painter = painterResource(id = iconResId)
            // 显示图标
            Image(
                // 图标绘制器
                painter = iconPainter,
                // 图标描述，用于无障碍访问
                contentDescription = text,
                // 设置图标的大小
                modifier = Modifier.size(24.dp),
                // 设置图标内容的缩放方式
                contentScale = ContentScale.Fit,
                // 根据选中状态设置图标的颜色
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(contentColor)
            )
            // 添加一个间隔
            Spacer(modifier = Modifier.height(4.dp))
            // 显示说明文字
            Text(
                // 文字内容
                text = text,
                // 设置文字样式
                style = TextStyle(
                    // 文字大小
                    fontSize = 12.sp,
                    // 文字粗细，选中时加粗，未选中时正常
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    // 文字颜色，根据选中状态设置
                    color = contentColor
                )
            )
        }
    }
}
