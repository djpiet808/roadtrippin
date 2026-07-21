create schema if not exists roadtrippin;

revoke all on schema roadtrippin from public, anon;
grant usage on schema roadtrippin to authenticated, service_role;

create table roadtrippin.trips (
    user_id uuid not null references auth.users(id) on delete cascade,
    id uuid not null,
    name text not null default '',
    destination text not null default '',
    vehicle text not null default '',
    notes text not null default '',
    started_at timestamptz not null,
    ended_at timestamptz,
    start_latitude double precision check (start_latitude between -90 and 90),
    start_longitude double precision check (start_longitude between -180 and 180),
    start_place_label text,
    end_latitude double precision check (end_latitude between -90 and 90),
    end_longitude double precision check (end_longitude between -180 and 180),
    end_place_label text,
    modified_at timestamptz not null,
    mutation_id uuid not null,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, id)
);

create table roadtrippin.travelers (
    user_id uuid not null references auth.users(id) on delete cascade,
    id uuid not null,
    trip_id uuid not null,
    name text not null check (length(trim(name)) > 0),
    modified_at timestamptz not null,
    mutation_id uuid not null,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, id),
    foreign key (user_id, trip_id) references roadtrippin.trips(user_id, id) on delete cascade
);

create table roadtrippin.plate_sightings (
    user_id uuid not null references auth.users(id) on delete cascade,
    id uuid not null,
    trip_id uuid not null,
    jurisdiction_code text not null check (jurisdiction_code in (
        'AL','AK','AZ','AR','CA','CO','CT','DE','DC','FL','GA','HI','ID','IL','IN','IA','KS','KY','LA','ME','MD','MA','MI','MN','MS','MO','MT','NE','NV','NH','NJ','NM','NY','NC','ND','OH','OK','OR','PA','RI','SC','SD','TN','TX','UT','VT','VA','WA','WV','WI','WY','BC','MX'
    )),
    first_seen_at timestamptz not null,
    latitude double precision check (latitude between -90 and 90),
    longitude double precision check (longitude between -180 and 180),
    place_label text,
    modified_at timestamptz not null,
    mutation_id uuid not null,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, id),
    unique (user_id, trip_id, jurisdiction_code),
    foreign key (user_id, trip_id) references roadtrippin.trips(user_id, id) on delete cascade
);

create table roadtrippin.journal_entries (
    user_id uuid not null references auth.users(id) on delete cascade,
    id uuid not null,
    trip_id uuid not null,
    entry_type text not null check (entry_type in ('note', 'stop')),
    title text not null default '',
    body text not null default '',
    occurred_at timestamptz not null,
    latitude double precision check (latitude between -90 and 90),
    longitude double precision check (longitude between -180 and 180),
    place_label text,
    modified_at timestamptz not null,
    mutation_id uuid not null,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, id),
    foreign key (user_id, trip_id) references roadtrippin.trips(user_id, id) on delete cascade
);

create table roadtrippin.tags (
    user_id uuid not null references auth.users(id) on delete cascade,
    id uuid not null,
    name text not null check (length(trim(name)) > 0),
    modified_at timestamptz not null,
    mutation_id uuid not null,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, id)
);

create unique index tags_user_name_key on roadtrippin.tags (user_id, lower(trim(name)));

create table roadtrippin.journal_entry_tags (
    user_id uuid not null references auth.users(id) on delete cascade,
    entry_id uuid not null,
    tag_id uuid not null,
    quantity integer not null check (quantity > 0),
    modified_at timestamptz not null,
    mutation_id uuid not null,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, entry_id, tag_id),
    foreign key (user_id, entry_id) references roadtrippin.journal_entries(user_id, id) on delete cascade,
    foreign key (user_id, tag_id) references roadtrippin.tags(user_id, id) on delete cascade
);

create table roadtrippin.journal_photos (
    user_id uuid not null references auth.users(id) on delete cascade,
    id uuid not null,
    trip_id uuid not null,
    entry_id uuid not null,
    storage_path text not null,
    sort_order smallint not null check (sort_order between 0 and 4),
    width integer check (width > 0),
    height integer check (height > 0),
    modified_at timestamptz not null,
    mutation_id uuid not null,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, id),
    unique (storage_path),
    unique (user_id, entry_id, sort_order),
    foreign key (user_id, trip_id) references roadtrippin.trips(user_id, id) on delete cascade,
    foreign key (user_id, entry_id) references roadtrippin.journal_entries(user_id, id) on delete cascade,
    check (storage_path = user_id::text || '/' || trip_id::text || '/' || entry_id::text || '/' || id::text)
);

create table roadtrippin.achievement_awards (
    user_id uuid not null references auth.users(id) on delete cascade,
    id uuid not null,
    achievement_id text not null,
    trip_id uuid,
    earned_at timestamptz not null,
    modified_at timestamptz not null,
    mutation_id uuid not null,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, id),
    foreign key (user_id, trip_id) references roadtrippin.trips(user_id, id) on delete cascade
);

create unique index achievement_awards_trip_key
    on roadtrippin.achievement_awards (user_id, achievement_id, coalesce(trip_id, '00000000-0000-0000-0000-000000000000'::uuid));

create table roadtrippin.sync_tombstones (
    user_id uuid not null references auth.users(id) on delete cascade,
    entity_type text not null check (entity_type in ('trip','traveler','plate_sighting','journal_entry','tag','journal_entry_tag','journal_photo','achievement_award')),
    entity_id uuid not null,
    deleted_at timestamptz not null,
    mutation_id uuid not null,
    server_updated_at timestamptz not null default now(),
    primary key (user_id, entity_type, entity_id)
);

create index travelers_trip_idx on roadtrippin.travelers (user_id, trip_id);
create index plate_sightings_trip_idx on roadtrippin.plate_sightings (user_id, trip_id, first_seen_at);
create index journal_entries_trip_idx on roadtrippin.journal_entries (user_id, trip_id, occurred_at desc);
create index journal_photos_entry_idx on roadtrippin.journal_photos (user_id, entry_id);
create index journal_photos_trip_idx on roadtrippin.journal_photos (user_id, trip_id);
create index achievement_awards_trip_idx on roadtrippin.achievement_awards (user_id, trip_id);
create index sync_tombstones_updated_idx on roadtrippin.sync_tombstones (user_id, server_updated_at);

create or replace function roadtrippin.touch_server_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.server_updated_at = now();
    return new;
end;
$$;

do $$
declare
    table_name text;
begin
    foreach table_name in array array[
        'trips','travelers','plate_sightings','journal_entries','tags',
        'journal_entry_tags','journal_photos','achievement_awards','sync_tombstones'
    ] loop
        execute format(
            'create trigger %I before update on roadtrippin.%I for each row execute function roadtrippin.touch_server_updated_at()',
            table_name || '_touch_server_updated_at', table_name
        );
        execute format('alter table roadtrippin.%I enable row level security', table_name);
        execute format('alter table roadtrippin.%I force row level security', table_name);
        execute format(
            'create policy %I on roadtrippin.%I for select to authenticated using ((select auth.uid()) = user_id)',
            table_name || '_owner_select', table_name
        );
        execute format(
            'create policy %I on roadtrippin.%I for insert to authenticated with check ((select auth.uid()) = user_id)',
            table_name || '_owner_insert', table_name
        );
        execute format(
            'create policy %I on roadtrippin.%I for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id)',
            table_name || '_owner_update', table_name
        );
        execute format(
            'create policy %I on roadtrippin.%I for delete to authenticated using ((select auth.uid()) = user_id)',
            table_name || '_owner_delete', table_name
        );
    end loop;
end;
$$;

revoke all on all tables in schema roadtrippin from public, anon;
grant select, insert, update, delete on all tables in schema roadtrippin to authenticated, service_role;
revoke all on all functions in schema roadtrippin from public, anon, authenticated;
grant execute on function roadtrippin.touch_server_updated_at() to service_role;

alter default privileges for role postgres in schema roadtrippin revoke all on tables from public, anon;
alter default privileges for role postgres in schema roadtrippin grant select, insert, update, delete on tables to authenticated, service_role;
alter default privileges for role postgres in schema roadtrippin revoke execute on functions from public, anon, authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'roadtrippin-journal-photos',
    'roadtrippin-journal-photos',
    false,
    10485760,
    array['image/jpeg', 'image/png', 'image/heic', 'image/heif']
)
on conflict (id) do update set
    public = false,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

create policy roadtrippin_photo_owner_select
on storage.objects for select to authenticated
using (
    bucket_id = 'roadtrippin-journal-photos'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

create policy roadtrippin_photo_owner_insert
on storage.objects for insert to authenticated
with check (
    bucket_id = 'roadtrippin-journal-photos'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and array_length(storage.foldername(name), 1) = 4
);

create policy roadtrippin_photo_owner_update
on storage.objects for update to authenticated
using (
    bucket_id = 'roadtrippin-journal-photos'
    and (storage.foldername(name))[1] = (select auth.uid())::text
)
with check (
    bucket_id = 'roadtrippin-journal-photos'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and array_length(storage.foldername(name), 1) = 4
);

create policy roadtrippin_photo_owner_delete
on storage.objects for delete to authenticated
using (
    bucket_id = 'roadtrippin-journal-photos'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

alter role authenticator set pgrst.db_schemas = 'public,graphql_public,roadtrippin';
notify pgrst, 'reload config';
