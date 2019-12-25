document.observe('dom:loaded', function () {
    jQuery('.chosen-select').each(function() {
        jQuery(this).chosen({include_group_label_in_selected: true, search_contains: true});
    });
});