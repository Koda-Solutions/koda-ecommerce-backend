# CloudWatch log group for the container logs (awslogs not used on the bare
# instance, but /var/log/ecom-* and docker logs are the window into the stack)
# plus a status-check alarm so the owner is told sooner rather than later.

resource "aws_cloudwatch_log_group" "ecom" {
  name              = "/ecom/dev/app"
  retention_in_days = 14
  tags              = { Name = "ecom-logs" }
}

resource "aws_sns_topic" "alerts" {
  name = "ecom-alerts"
  tags = { Name = "ecom-alerts" }
}

resource "aws_cloudwatch_metric_alarm" "instance_status" {
  alarm_name          = "ecom-instance-status-check"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "StatusCheckFailed"
  namespace           = "AWS/EC2"
  period              = "300"
  statistic           = "Maximum"
  threshold           = "0"
  alarm_description   = "EC2 status check failed twice in a row on the ecommerce host"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    InstanceId = aws_instance.app.id
  }
}

resource "aws_sns_topic_subscription" "alerts_email" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.owner_email
}