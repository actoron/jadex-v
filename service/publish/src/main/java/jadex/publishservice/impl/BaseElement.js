export class BaseElement extends HTMLElement 
{
	static appclass = null;

	static setAppClass(clazz) 
	{
		BaseElement.appclass = clazz;
	}
	
	constructor() 
	{
		super();
		if (!BaseElement.appclass) 
			throw new Error("App class not set. Call BaseElement.setAppClass(MyApp) first.");

		this.attachShadow({ mode: 'open' });
		this.visible = true;
		this.stylesLoaded = false;
		
		this.app = BaseElement.appclass.getInstance();

		this.listener = (type, value) => this.appChanged(type, value);

		this.loadStyle("style.css")
		.then(() => {
			this.stylesLoaded = true;
			this.update();
		})
		.catch(err => {
			this.setError(err);
		});
	}

	connectedCallback() 
	{
		this.app.addListener(this.listener);
		if (this.stylesLoaded) 
			this.update();
	}

	disconnectedCallback() 
	{
		this.app.removeListener(this.listener);
	}

	appChanged(type, value) 
	{
		// To be overridden by subclasses
	}

	update() 
	{
		if (this.shadowRoot) 
			this.shadowRoot.innerHTML = this.getHTML();
	}

	getHTML() 
	{
		throw new Error('You have to implement the method getHTML() in your component');
	}

	setVisible(visible) 
	{
		this.visible = visible;
		this.update();
	}

	isVisible() 
	{
		return this.visible;
	}

	setError(err) 
	{
		this.app.setError(err);
	}

	setErrorText(text) 
	{
		this.app.setErrorText(text);
	}

	setInfoText(text) 
	{
		this.app.setInfoText(text);
	}
	
	getApp()
	{
	    return this.app;
	}

	async loadStyle(url) 
	{
		try 
		{
			const response = await fetch(url);
			const css = await response.text();
			return this.addStyle(css);
		} 
		catch (err) 
		{
			throw err;
		}
	}

	addStyle(css) 
	{
		const sheet = new CSSStyleSheet();
		sheet.replaceSync(css);
		this.shadowRoot.adoptedStyleSheets = [...this.shadowRoot.adoptedStyleSheets, sheet];
		return sheet;
	}

	static getHost(node) 
	{
		while (node && !node.host && node.parentNode) 
			node = node.parentNode;
		return node?.host || node;
	}
}

export default BaseElement;
