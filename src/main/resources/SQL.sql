create database if not exists oh_db;
use oh_db;
CREATE TABLE task_sample (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            chose_node_id BIGINT,
                            duration DOUBLE NOT NULL
);
CREATE TABLE node_status (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            cpu_util DOUBLE,
                            mem_free DOUBLE,
                            power_remain DOUBLE,
                            storage_ratio DOUBLE,
                            task_sample_id BIGINT
);