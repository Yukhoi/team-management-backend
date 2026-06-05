alter table audit.operation_log
    add column if not exists operator_user_id bigint,
    add column if not exists operator_username varchar(100);
