### General process
The full general process to create all the ingredients for a plandefinition is as follows

1) CQL --> FHIR JSON Library (Through refresh script)
2) FHIR JSON Library --> valueset.db (Through `src/cql/r4/utils/updateValueSetDB.js`)
3) valueset-db.json --> VSAC xlxs (Through manual searches)
4) VSAC xlxs --> VSAC JSON (Through CQF Tooling)


### CQL  --> JSON 

The CQL File can be made into a JSON FHIR library by running the refresh script (.sh or .bat). Currently under investigation. See Alex for more details.

### JSON --> valueset-db.json

Happens by running `src/cql/r4/utils/updateValueSetDB.js`. Some contents of note are 

```javascript
const r4FactorsELM = require('../cql/r4/Factors_to_Consider_in_Managing_Chronic_Pain_FHIRv400.json');
const r4CommonsELM = require('../cql/r4/CDS_Connect_Commons_for_FHIRv400.json');
const r4HelpersELM = require('../cql/r4/FHIRHelpers.json');
```

which instantiate necessary FHIR libraries. In this case, the script is hard coded to use the Pain library, but it should be easy to change it at will. The following method seems to actually write to valueset-db.json, although the entire program seems easily repurposable.

```javascript
const tempDBFile = path.join(tempFolder, 'valueset-db.json');
    const original = JSON.parse(fs.readFileSync(tempDBFile, 'utf8'));
    let oidKeys = Object.keys(original).sort();
    console.log(`Loaded ${oidKeys.length} value sets`);
    console.log('Translating JSON to expected format')
    const fixed = {};
    for (const oid of oidKeys) {
      fixed[oid] = {};
      for (const version of Object.keys(original[oid])) {
        fixed[oid][version] = original[oid][version]['codes'].sort((a, b) => {
          if (a.code < b.code) return -1;
          else if (a.code > b.code) return 1;
          return 0;
        });
      }
    }
```


### valueset.db --> VSAC xlxs
The format of valueset-db.json is as follows:
```
{
  "2.16.840.1.113762.1.4.1032.102": { //The ID of a valueset
    "20200128": [
      {
        "code": "219125007",
        "system": "http://snomed.info/sct",
        "version": "2019-09"
      },
      {
        "code": "219126008",
        "system": "http://snomed.info/sct",
        "version": "2019-09"
      }
      //...
     ]
  },
  "2.16.840.1.113762.1.4.1032.26": { //another valueset ID
    "20200211": [
      {
        "code": "1000048",
        "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
        "version": "2020-02"
      }
      //...
    ]
  }
}
```

Each element in the JSON is a valueset, which makes it trivial to read.
The valuesets can be found at VSAC, and can be searched by id (found in valueset.db) at https://vsac.nlm.nih.gov/valueset/expansions?pr=all.
This should be a simple web scraper.


### VSAC xlxs --> VSAC JSON

To convert a xlxs to json, you need to run CQF Tooling's build command, which is
` mvn exec:java -Dexec.args="[-VsacXlsxToValueSet] [-pathtospreadsheet | -pts] (-outputpath | -op)"`.
An example in action is 
`mvn exec:java -Dexec.args="-VsacXlsxToValueSet -pts=/Users/paulpuscas/Desktop/PainVSAC/.xlxs -op=/Users/paulpuscas/Desktop/PainJSONs/"`

Since this is a command, it should be easy to automate with a shell script.

