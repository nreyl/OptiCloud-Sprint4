import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';

import { Company, CompanyDocument } from './company.schema';

@Injectable()
export class CompanyService {
  constructor(
    @InjectModel(Company.name) private readonly companyModel: Model<CompanyDocument>,
  ) {}

  async findByCode(code: string): Promise<CompanyDocument | null> {
    return this.companyModel.findOne({ code }).exec();
  }

  async ensure(code: string, name: string): Promise<CompanyDocument> {
    return this.companyModel
      .findOneAndUpdate({ code }, { code, name, active: true }, { upsert: true, new: true })
      .exec();
  }
}
