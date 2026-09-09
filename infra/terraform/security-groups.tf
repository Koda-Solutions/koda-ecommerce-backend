# The only public surface is Caddy on 80 and 443. No SSH at all: operations go
# through Session Manager, where IAM decides who may connect and CloudTrail
# records that they did, exactly as in the Koda infra.

resource "aws_security_group" "app" {
  name        = "ecom-app"
  description = "Caddy edge only; everything else stays inside the Docker network"
  vpc_id      = aws_vpc.ecom.id
  tags        = { Name = "ecom-app-sg" }
}

resource "aws_security_group_rule" "https_in" {
  type              = "ingress"
  security_group_id = aws_security_group.app.id
  description       = "HTTPS from the internet"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "http_in" {
  type              = "ingress"
  security_group_id = aws_security_group.app.id
  description       = "HTTP redirect from the internet"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "egress_all" {
  type              = "egress"
  security_group_id = aws_security_group.app.id
  description       = "Outbound for pulls, ACME and the SSM agent"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}