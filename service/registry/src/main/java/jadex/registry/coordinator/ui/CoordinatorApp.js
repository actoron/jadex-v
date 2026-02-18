import App from './api/App.js';

class CoordinatorApp extends App
{
	constructor() 
  	{
		super();
    	this.registries = [];
  	}

	addRegistry(r) 
  	{
    	this.registries.push(r);
    	this.notify("registries", this.registries);
  	}
  
  	removeRegistry(r) 
  	{
		// todo: use also providerid
    	this.registries = this.registries.filter(reg => 
			r.service.serviceName !== reg.service.serviceName || 
			r.service.providerId !== reg.service.providerId);
    	this.notify("registries", this.registries);
  	}

  	getRegistries() 
  	{
  		return this.registries;
  	}
	
	clearRegistries()
	{
		this.registries = [];
        this.notify("registries", this.registries);		
	}

  	findRegistryById(cid) 
  	{
  		return this.registries.find(r => r.cid === cid);
  	}
}

export default CoordinatorApp;