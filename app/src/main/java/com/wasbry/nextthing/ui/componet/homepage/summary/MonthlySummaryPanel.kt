// 导入 Android 系统版本相关的类，用于检查系统版本要求
import android.os.Build
// 导入注解，用于指定函数运行所需的最低 Android 系统版本
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
// 导入 Box 布局组件，用于包裹和定位子组件
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
// 导入 Spacer 组件，用于在布局中创建空白间距
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
// 导入用于创建圆角形状的类
import androidx.compose.foundation.shape.RoundedCornerShape
// 导入 Material Design 3 中的卡片组件
import androidx.compose.material3.Card
// 导入 Material Design 3 中卡片的默认设置类
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wasbry.nextthing.database.model.MonthlySummary
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthlySummaryPanel(
    monthlySummary: MonthlySummary
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MM-dd")

    // 使用 Card 组件创建一个卡片式的容器
    Card(
        modifier = Modifier
            .width(400.dp)
            .padding(16.dp),
        // 设置卡片的形状为圆角，圆角半径为 12dp
        shape = RoundedCornerShape(12.dp),
        // 设置卡片的阴影效果，默认阴影高度为 8dp
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        // 设置卡片的颜色，容器背景色为浅灰色
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            // 设置子组件之间的垂直间距为 8dp
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "本月记录",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                // 设置子组件在水平方向上均匀分布
                horizontalArrangement = Arrangement.SpaceBetween,
                // 设置子组件在垂直方向上居中对齐
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 使用 Box 组件包裹日期显示区域
                Box(
                    // 设置 Box 的修饰符，包括宽度、背景颜色、形状和内边距
                    modifier = Modifier
                        .width(120.dp)
                        .background(color = Color(0xFFE0E0E0), shape = RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    // 在 Box 内部使用 Row 组件水平排列日期文本
                    Row(
                        // 设置子组件在水平方向上均匀分布
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        // 设置子组件在垂直方向上居中对齐
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 显示开始日期
                        Text(
                            text = monthlySummary.startDate.format(dateFormatter),
                            // 设置文本的样式，包括字体大小和颜色
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        )
                        // 显示分隔符 " - "
                        Text(
                            text = " - ",
                            // 设置文本的样式，包括字体大小和颜色
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        )
                        // 显示结束日期
                        Text(
                            text = monthlySummary.endDate.format(dateFormatter),
                            // 设置文本的样式，包括字体大小和颜色
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        )
                    }
                }

                // 显示规划小时数
                Text(
                    text = monthlySummary.plannedHours.toString(),
                    // 设置文本的样式，包括字体大小、粗细和颜色
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                )
            }

            // 创建一个高度为 16dp 的空白间距
            Spacer(modifier = Modifier.height(16.dp))

            // 使用 Row 组件进行水平布局
            Row(
                // 设置 Row 的修饰符，填充最大宽度并设置高度为 32dp
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                // 设置子组件在水平方向上均匀分布
                horizontalArrangement = Arrangement.SpaceBetween,
                // 设置子组件在垂直方向上居中对齐
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 显示预计完成任务数
                Text(
                    text = "预计完成 ${monthlySummary.expectedTaskCount}",
                    // 设置文本的样式，包括字体大小和颜色
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                )
                // 显示实际完成任务数
                Text(
                    text = "实际完成 ${monthlySummary.actualTaskCount}",
                    // 设置文本的样式，包括字体大小和颜色
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFF666666)
                    )
                )
            }
        }
    }
}