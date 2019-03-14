# Portal Curator Bot

This demo was shown at the Esri Dev Summit 2019. It is a python script that, when evoked on AWS lambda from ArcGIS Enterprise Webhooks, acts as a "Curator Bot" to attempt to fill in missing metadata on portal items.

Add the `arcgis` python package folder from the most recent build of the ArcGIS API for Python into this directory, and zip everything up. If you upload that archive to AWS Lambda, link up Lambda to a POST endpoint via AWS Gateway, and link that Post URL to ArcGIS Enteprise Webhooks, you should get the behavior seen in the plenary demo. See [this blog post](https://www.esri.com/arcgis-blog/products/arcgis-enterprise/administration/webhooks-dev-summit-2019/) for more information. 
