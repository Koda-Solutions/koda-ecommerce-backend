-- Koda Ecommerce, one schema and one least privilege user per service (SEC-06).
-- The skeleton init proves no cross schema grant: each user can access exactly
-- its own ecommerce_* schema and nothing else.

CREATE DATABASE IF NOT EXISTS ecommerce_user;
CREATE DATABASE IF NOT EXISTS ecommerce_product;
CREATE DATABASE IF NOT EXISTS ecommerce_cart;
CREATE DATABASE IF NOT EXISTS ecommerce_order;
CREATE DATABASE IF NOT EXISTS ecommerce_payment;
CREATE DATABASE IF NOT EXISTS ecommerce_shipment;
CREATE DATABASE IF NOT EXISTS ecommerce_notification;

CREATE USER IF NOT EXISTS 'koda_user_ms'@'%' IDENTIFIED BY 'koda_user_ms_pw';
CREATE USER IF NOT EXISTS 'koda_product_ms'@'%' IDENTIFIED BY 'koda_product_ms_pw';
CREATE USER IF NOT EXISTS 'koda_cart_ms'@'%' IDENTIFIED BY 'koda_cart_ms_pw';
CREATE USER IF NOT EXISTS 'koda_order_ms'@'%' IDENTIFIED BY 'koda_order_ms_pw';
CREATE USER IF NOT EXISTS 'koda_payment_ms'@'%' IDENTIFIED BY 'koda_payment_ms_pw';
CREATE USER IF NOT EXISTS 'koda_shipment_ms'@'%' IDENTIFIED BY 'koda_shipment_ms_pw';
CREATE USER IF NOT EXISTS 'koda_notification_ms'@'%' IDENTIFIED BY 'koda_notification_ms_pw';

GRANT ALL PRIVILEGES ON ecommerce_user.* TO 'koda_user_ms'@'%';
GRANT ALL PRIVILEGES ON ecommerce_product.* TO 'koda_product_ms'@'%';
GRANT ALL PRIVILEGES ON ecommerce_cart.* TO 'koda_cart_ms'@'%';
GRANT ALL PRIVILEGES ON ecommerce_order.* TO 'koda_order_ms'@'%';
GRANT ALL PRIVILEGES ON ecommerce_payment.* TO 'koda_payment_ms'@'%';
GRANT ALL PRIVILEGES ON ecommerce_shipment.* TO 'koda_shipment_ms'@'%';
GRANT ALL PRIVILEGES ON ecommerce_notification.* TO 'koda_notification_ms'@'%';

FLUSH PRIVILEGES;