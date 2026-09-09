data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# The only instance in this project. Caddy is the only open port; Docker runs
# the rest, images from ECR. The elastic IP is baked into the DNS record so a
# stop and start never breaks the certificate.
resource "aws_instance" "app" {
  ami                         = data.aws_ami.al2023.id
  instance_type               = var.instance_type
  subnet_id                   = aws_subnet.public[0].id
  vpc_security_group_ids      = [aws_security_group.app.id]
  iam_instance_profile        = aws_iam_instance_profile.instance.name
  user_data                   = replace(file("${path.module}/user-data.sh"), "__OWNER_EMAIL__", var.owner_email)
  user_data_replace_on_change = true
  associate_public_ip_address = true

  root_block_device {
    volume_size = 30
    volume_type = "gp3"
  }

  tags = { Name = "ecom-dev-app" }
}

resource "aws_eip" "app" {
  domain = "vpc"
  tags   = { Name = "ecom-dev-eip" }
}

resource "aws_eip_association" "app" {
  instance_id   = aws_instance.app.id
  allocation_id = aws_eip.app.id
}