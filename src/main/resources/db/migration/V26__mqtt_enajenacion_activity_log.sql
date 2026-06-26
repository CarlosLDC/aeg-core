create table if not exists public.mqtt_enajenacion_activity (
  id varchar(36) not null,
  recorded_at timestamp with time zone not null,
  mac varchar(12) not null,
  printer_id bigint null,
  ptr_reg varchar(32) null,
  direction varchar(16) null,
  topic text null,
  payload text null,
  result varchar(16) not null,
  detail text null,
  session_state varchar(32) null,
  constraint mqtt_enajenacion_activity_pkey primary key (id),
  constraint mqtt_enajenacion_activity_printer_fkey
    foreign key (printer_id) references public.impresoras (id) on delete set null
);

create index if not exists idx_mqtt_enajenacion_activity_recorded_at
  on public.mqtt_enajenacion_activity (recorded_at desc);

create index if not exists idx_mqtt_enajenacion_activity_mac
  on public.mqtt_enajenacion_activity (mac);

create index if not exists idx_mqtt_enajenacion_activity_result
  on public.mqtt_enajenacion_activity (result);

create index if not exists idx_mqtt_enajenacion_activity_ptr_reg
  on public.mqtt_enajenacion_activity (ptr_reg);
