# WA

WA is an evidence-based gene prioritization algorithm based on a Bayesian diagnostic model. WA uses only proband phenotype and the HPO ontology to provide the prioritization.

## Running WA
Most users should download the latest WA distribution from the [Releases page](https://github.com/matevz-kovacic/WA/releases) unpack, and run it.


## Preprint
Preprint of the paper describing WA is available [here.](https://github.com/matevz-kovacic/WA/blob/master/WA-preprint.pdf)


## Help
For help and instructions on using the program, run WA without arguments.

## Example of gene prioritization

After unpacking WA, try to prioritize the top 10 most likely pathogenic genes of the proband by enumerating the HPO codes of his phenotype, e.g.:
```
java -jar WA.jar -m 10 HP:0002376 HP:0002353 HP:0002240 HP:0010780 HP:0010729 HP:0001250
```

The result of prioritizing the genes is presented in an Excel heatmap. The column values can be interpreted as votes of a proband's phenotypic sign for the pathogenicity of the genes (see section 2.6 of the [preprint](https://github.com/matevz-kovacic/WA/blob/master/WA-preprint.pdf) for the details).

![loyee data](https://github.com/matevz-kovacic/WA/blob/master/heatmap.png "heatmap of gene prioritization") 
