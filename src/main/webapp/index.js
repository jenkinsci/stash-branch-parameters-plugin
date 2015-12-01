document.observe('dom:loaded', function () {
    var elements = $$('.chosen-select');
    for (var i = 0; i < elements.length; i++){
        new Chosen(elements[i], {include_group_label_in_selected: true, search_contains: true});
    }
});