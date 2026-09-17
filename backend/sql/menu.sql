-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('饮食记录', '3', '1', '/diet/record', 'C', '0', 'diet:record:view', '#', 'admin', sysdate(), '', null, '饮食记录菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('饮食记录查询', @parentId, '1',  '#',  'F', '0', 'diet:record:list',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('饮食记录新增', @parentId, '2',  '#',  'F', '0', 'diet:record:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('饮食记录修改', @parentId, '3',  '#',  'F', '0', 'diet:record:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('饮食记录删除', @parentId, '4',  '#',  'F', '0', 'diet:record:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, url, menu_type, visible, perms, icon, create_by, create_time, update_by, update_time, remark)
values('饮食记录导出', @parentId, '5',  '#',  'F', '0', 'diet:record:export',       '#', 'admin', sysdate(), '', null, '');
