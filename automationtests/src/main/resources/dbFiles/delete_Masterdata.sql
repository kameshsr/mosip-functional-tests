delete from master.blocklisted_words where word='dumbo6';
delete from master.blocklisted_words where word='dumbo7';
delete from master.machine_master where id='123';
delete from master.machine_type where code='Laptop-2';
delete from master.machine_spec where id='HP04';
delete FROM master.gender where code='Genderdummy';
delete FROM master.device_master where name='testDevicedummy';
delete FROM master.device_spec where id='743';
delete FROM master.device_type where code='GST3';
delete FROM master.loc_holiday where holiday_name='AutoTest user Eng';
delete FROM master.reg_center_type where code='ALT-3';
delete FROM master.registration_center where name='Test123';
delete from master.loc_holiday where holiday_name in('AutoTest user Ara','AutoTest user Ara');
delete from master.reg_center_type where code in('ALT-3','ALT-5');
delete from master.registration_center where id='10000';
delete from master.device_type where code in ('GST3','GST4');
delete from master.doc_type where code in ('TestDocType0010','TestDocType0020');
delete from master.doc_category where code in ('DocTestCode123','DocTestCode321');
delete FROM master.location where code='TST12';
delete from master.template_type where code='Test-info-Template-auto';
update master.location set is_active='true', is_deleted='false' where code='10114';
update master.template set is_active='true', is_deleted='false' where id='1101';
delete from master.location where code in('TST123','IND');
delete from master.template where id='445566777';
delete from master.valid_document where docTypeCode='doc_auto_test';
delete from master.user_detail where cr_by='110005';
delete from master.template_type where code='Test-info-Template-auto';
delete from master.template_file_format where code='Doc';
delete from master.reason_list where code='TEST_LIST_CODE';
delete from master.reason_category where code='TEST_CAT_CODE';
delete from master.language where code='hin';
delete from master.identity_schema where title='test-schema';
delete from master.biometric_attribute where code='TST';
delete from master.biometric_type where code='dumbo6';
delete from master.appl_form_type where code='dumbo';
delete from master.id_type where code='NEW';
delete from master.dynamic_field where (name='TestAutomationField' and name='TestAPL');
