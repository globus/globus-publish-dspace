import os
from argparse import ArgumentParser
import sys
import csv
from lxml import etree
import time

# Run with
#python create_schema_form.py --name miccom --namespace http://globus.org/publication-schemas/miccom/0.1 --title "Miccom schema" --input_file miccom.csv --output_file miccom --datacite_file ../src/dspace/config/forms/input-forms-datacite-Mandatory.xml 
#
# Expects CSV with a header and using the same attributes as exist in the schema/forms file. E.g., 
#
# schema,element,label,input-type,repeatable,required,hint
# miccom,scriptlanguage,Script Language,onebox,FALSE,FALSE,Enter the script language used
#
#
# Place the resulting schema in dspace/config/registeries
# Place the resulting input form in dspace/config/forms/custom
#
# Run the following to load the schema (from the dspace/target/dsipace-X.X-build dir
# ant load_schema -Dschema-file=<output_file>-metadata.xml
 
def parse_cli():
    parser = ArgumentParser(description="Create schema and input form")
    parser.add_argument('--name', required=True)
    parser.add_argument('--namespace', required=True)
    parser.add_argument('--title', required=True)
    parser.add_argument('--input_file', required=True)
    parser.add_argument('--output_file', required=True)
    parser.add_argument('--datacite_file', required=False)
    return parser.parse_args()

def parse_input_file(file_name):
    schema_elements = []
    with open(file_name, 'rb') as f:
        reader = csv.reader(f)
    	labels  = next(reader)
        print labels
        for row in reader:
	    element = {}
            for i, v in enumerate(labels):
                element[labels[i]] = row[i]
            schema_elements.append(element)
    return schema_elements

def get_value(k, obj, required=True):
    if k in obj:
        return obj[k]
    if required:
        print "Key %s is required" % k
    return ""

def get_bool(k, obj, required=True):
    if k in obj:
        if check_bool(obj[k]):
            return "true"
        return "false"
    if required:
        print "Key %s is required" % k
    return "false"

def is_required(obj):
    if "required" in obj and check_bool(obj["required"]):
        return True
    return False 

def check_bool(val):
    return val in ['t', 'T', 'True', 'true', 'TRUE']

def create_schema(schema_name, schema_namespace, schema_title, schema_elements):
    root = etree.Element("dspace-dc-types")
   
    dc_schema = etree.SubElement(root, "dc-schema")
    name = etree.SubElement(dc_schema, "name")
    namespace = etree.SubElement(dc_schema, "namespace")
    name.text = schema_name
    namespace.text = schema_namespace

    dspace_header = etree.SubElement(root, "dspace-header")
    title = etree.SubElement(dspace_header, "title")
    contributor = etree.SubElement(dspace_header, "contributor.author")
    date = etree.SubElement(dspace_header, "date.created")
    description = etree.SubElement(dspace_header, "description")
    title.text = schema_title
    contributor.text = "Automatically generated"
    date.text = time.strftime("%Y-%m-%d")
    description.text = ""

    for e in schema_elements:
        elem = etree.SubElement(root, "dc-type")
        schema = etree.SubElement(elem, "schema")
        element = etree.SubElement(elem, "element")
        qualifier = etree.SubElement(elem, "qualifier")
        note = etree.SubElement(elem, "note")
        schema.text = get_value("schema", e)
        element.text = get_value("element", e)
        qualifier.text = get_value("qualifier", e, False)
        note.text = get_value("note", e, False)

    return etree.tostring(root, pretty_print=True)

def load_datacite_form(form_file):
    parser = etree.XMLParser(remove_blank_text=True)
    root = etree.parse(form_file, parser).getroot()
    page = root.xpath("//page")
    for p in page:
        return p
    print "Page not found in imported datacite file %s" % form_file

def create_input_form(schema_name, schema_namespace, schema_title, schema_elements, datacite):
    form_name = "%sform" % (schema_name)

    root = etree.Element("input-forms")

    form_map = etree.SubElement(root, "form-map")
    name_map = etree.SubElement(form_map, "name-map")
    name_map.set("collection-handle", "default")
    name_map.set("form-name", form_name)

    form_definitions = etree.SubElement(root, "form-definitions")
    form = etree.SubElement(form_definitions,  "form")
    form.set("name", form_name)

    page_num = 1
    if datacite:
        form.insert(0, load_datacite_form(datacite))
        page_num += 1

    page = etree.SubElement(form, "page")
    page.set("number", "%d" % page_num)

    for e in schema_elements:
	field = etree.SubElement(page, "field")
        schema = etree.SubElement(field, "dc-schema")
        element = etree.SubElement(field, "dc-element")
        qualifier= etree.SubElement(field, "dc-qualifier")
        repeatable = etree.SubElement(field, "repeatable")
        label = etree.SubElement(field, "label")
        input_type = etree.SubElement(field, "input-type")
        hint = etree.SubElement(field, "hint")
        
        if is_required(e):
            required = etree.SubElement(field, "required")
            required.text = "You must enter a value for %s %s " % (get_value("element", e, False), get_value("qualifier", e, False))

        schema.text = get_value("schema", e)
        element.text = get_value("element", e)
        qualifier.text = get_value("qualifier", e, False)
        repeatable.text = get_bool("repeatable", e, False)
        label.text = get_value("label", e)
        input_type.text = get_value("input-type", e)
        hint.text = get_value("hint", e, False)
        
    return etree.tostring(root, pretty_print=True)

def main():
    args = parse_cli()
    schema_elements = parse_input_file(args.input_file)
    schema = create_schema(args.name, args.namespace, args.title, schema_elements)
    form = create_input_form(args.name, args.namespace, args.title, schema_elements, args.datacite_file)

    with open("%s-metadata.xml" % args.output_file, "w") as s:
        s.write(schema) 

    with open("input-forms-%s.xml" % args.output_file, "w") as f:
        f.write(form) 

if __name__ == '__main__':
    main()
