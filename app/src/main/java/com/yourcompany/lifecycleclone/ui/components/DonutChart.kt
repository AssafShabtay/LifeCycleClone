import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.minDimension
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.text.color
import com.yourcompany.lifecycleclone.ui.components.ChartSegment


@Composable
fun DonutChart(
    segments: List<ChartSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.15f
        var startAngle = -90f

        // Use inset to create padding for the stroke.
        // The drawing operations inside this block will be in the new, smaller bounds.
        inset(strokeWidth / 2) {
            segments.forEach { segment ->
                val sweep = segment.fraction * 360f
                drawArc(
                    color = segment.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
    }
}
