/**
 *
 * Copyright 2014-2016 The University of Chicago
 *
 * All rights reserved.
 */

$(document).ready(function() {
	function objectFromTag(script_tag, tag_name) {
		var tagVal = script_tag.attr(tag_name);
		if (tagVal) {
			tagVal = tagVal.replace(/'/g, "\"");
			var objVal = JSON.parse(tagVal);
			return objVal;
		} else {
			return {};
		}
	}

	function configureControls() {
		var sel = document.getElementById("provider-selector");
		var curSel = sel.options[sel.selectedIndex];
		var curSelClass = curSel.className;
		var curSelName = curSel.value;
		$( cur ).hide(250);
		if (curSelClass.indexOf("creator") != -1) {
			$( "#" + curSelName ).show(250);
			$( "#submit_delete_id").hide();
			$("#submit_create_id").show();
		} else {
			var newTitle = "";
			var newDisabled = false;
			if (curSelClass.indexOf("inuse") != -1) {
				newDisabled = true;
				newTitle = "Configuration is in use";
			} else if (curSelClass.indexOf("fromparent") != -1) {
				newDisabled = true;
				newTitle = "Configuration defined by parent Community";
			}
			$( "#submit_delete_id").show();
			$("#submit_create_id").hide();
			$('#submit_delete_id').prop('disabled', newDisabled);
			$('#submit_delete_id_div').prop('title', newTitle);
		}
		cur = "#" + curSelName;
	}

	var sel = document.getElementById("provider-selector");
	var options = sel.options;
	for (var i = 0; i < options.length; i++) {
		$( "#"+options[i].value).hide();
	}

	var curSel = options[sel.selectedIndex];
	cur = "#" + curSel.value;

	configureControls();

	$('#provider-selector').change(function() {
		var newSel = '#' + this.value;
		configureControls();
	});
});
