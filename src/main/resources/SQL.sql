create database if not exists oh_db;
use oh_db;
CREATE TABLE task_sample (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            task_id VARCHAR(255),
                            chose_node_id BIGINT,
                            duration DOUBLE NOT NULL
);
CREATE TABLE node_status (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            cpu_usage DOUBLE,
                            memory_usage DOUBLE,
                            power_remain DOUBLE,
                            storage_remain DOUBLE,
                            latency DOUBLE,
                            task_sample_id BIGINT
);