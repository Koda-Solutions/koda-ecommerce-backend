# The ecommerce test has no network database, so unlike Koda there are no
# private subnets: MySQL runs as a compose container on the same host with its
# data on the instance EBS volume. What matters for isolation is that nothing
# reaches the services directly; only Caddy is exposed, so the public subnets
# are enough.

resource "aws_vpc" "ecom" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags = { Name = "ecom-vpc" }
}

resource "aws_internet_gateway" "ecom" {
  vpc_id = aws_vpc.ecom.id
  tags   = { Name = "ecom-igw" }
}

resource "aws_subnet" "public" {
  count                   = 2
  vpc_id                  = aws_vpc.ecom.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true
  tags                    = { Name = "ecom-public-${count.index}" }
}

data "aws_availability_zones" "available" {
  state = "available"
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.ecom.id
  tags   = { Name = "ecom-public-rt" }
}

resource "aws_route" "public_internet" {
  route_table_id         = aws_route_table.public.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.ecom.id
}

resource "aws_route_table_association" "public" {
  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}