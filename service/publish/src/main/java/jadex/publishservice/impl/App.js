class App 
{
	#listeners = [];
	#connected = false;
	#error = null;
	
	/*static getInstance() 
	{
		if (!this.instance) 
			this.instance = new this();
		return this.instance;
	}*/
	
	static getInstance() 
	{
		if (!this.hasOwnProperty("instance")) 
			this.instance = new this(); 
		return this.instance;
	}
	
	addListener(listener) 
	{
		this.#listeners.push(listener);
	}

	removeListener(listener) 
	{
		this.#listeners = this.#listeners.filter(l => l !== listener);
	}

	get connected() 
	{
		return this.#connected;
	}

	set connected(value) 
	{
		this.#connected = value;
		this.notify("connected", value);
	}

	get error() 
	{
		return this.#error;
	}

	set error(value) 
	{
		this.#error = value;
		this.notify("error", value);
	}

	notify(type, value) 
	{
		this.#listeners.forEach(l => l(type, value));
	}

	showView(tagName, container = document.getElementById("view")) 
	{
		if (container) 
			container.innerHTML = `<${tagName}></${tagName}>`;
		else 
            console.error(`Container with id 'view' not found.`);
	}

	setError(err) 
	{
		this.setErrorText(this.formatError(err));
	}

	setErrorText(text) 
	{
		this.error = { text, error: true };
	}

	setInfoText(text) 
	{
		this.error = { text, error: false };
	}
	
	formatError(err) 
	{
		if (err?.response?.status === 401) 
			return "Wrong password";
		return err?.message || "Unknown error";
	}
}

export default App;