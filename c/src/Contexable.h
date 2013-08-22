

#ifndef Contexable__H___
#define Contexable__H___

class cJXCtx;

class Contexable{


public:
	cJXCtx* getCtxClass(){return ctxClass;}
	void setCtxClass(cJXCtx* c){this->ctxClass = c;}

private:
	cJXCtx* ctxClass;

};




#endif // ! Contexable__H___
